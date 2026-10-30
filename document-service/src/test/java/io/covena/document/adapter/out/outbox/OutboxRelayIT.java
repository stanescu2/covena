package io.covena.document.adapter.out.outbox;

import io.covena.document.KafkaTestcontainersConfiguration;
import io.covena.document.TestcontainersConfiguration;
import io.covena.document.application.port.in.ContentSource;
import io.covena.document.application.port.in.UploadDocumentCommand;
import io.covena.document.application.port.in.UploadDocumentUseCase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.covena.document.adapter.out.outbox.OutboxEventFixtures.DOCUMENT_UPLOADED;
import static io.covena.document.adapter.out.outbox.OutboxRelay.EVENT_ID_HEADER;
import static io.covena.document.adapter.out.outbox.OutboxRelay.EVENT_TYPE_HEADER;
import static io.covena.document.adapter.out.outbox.OutboxRelayConfiguration.DOCUMENT_EVENTS_TOPIC;
import static io.covena.document.domain.ContentType.PDF;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "covena.outbox.relay.fixed-delay=100ms")
@Import({TestcontainersConfiguration.class, KafkaTestcontainersConfiguration.class})
class OutboxRelayIT {
    private static final String PREFIX_GROUP_ID = "outbox-relay-it-";
    private static final byte[] PDF_CONTENT = "%PDF-1.7\nAcesta este un fișier de test".getBytes(StandardCharsets.UTF_8);
    private static final String TEST_FILE_NAME = "contract.pdf";
    private static final String QUERY_OUTBOX_EVENT_ID = "SELECT id FROM outbox_events WHERE aggregate_id = ?";
    private static final String QUERY_OUTBOX_PUBLISHED_AT = "SELECT published_at FROM outbox_events WHERE aggregate_id = ?";
    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("covena.storage.local.base-dir", () -> tempDir.toString());
    }

    @Autowired
    private UploadDocumentUseCase useCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KafkaContainer container;

    private ContentSource source;
    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        var properties = KafkaTestUtils.consumerProps(container.getBootstrapServers(),
            PREFIX_GROUP_ID + UUID.randomUUID(),
            false);
        properties.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(List.of(DOCUMENT_EVENTS_TOPIC));
        source = () -> new ByteArrayInputStream(PDF_CONTENT);
    }

    private ConsumerRecord<String, String> awaitRecordFor(UUID documentId) {
        var received = new ArrayList<ConsumerRecord<String, String>>();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            consumer.poll(Duration.ofMillis(200)).forEach(received::add);
            assertThat(received).anySatisfy(r -> assertThat(r.key()).isEqualTo(documentId.toString()));
        });
        return received.stream()
            .filter(r -> r.key().equals(documentId.toString()))
            .findFirst()
            .orElseThrow();
    }

    @Test
    void shouldPublishUploadedDocumentToKafka() throws JSONException {
        //given
        var command = new UploadDocumentCommand(TEST_FILE_NAME, PDF_CONTENT.length, PDF.getMimeType(), source);

        //when
        var result = useCase.uploadDocument(command);

        //then
        UUID documentId = result.id().uuidValue();
        var record = awaitRecordFor(documentId);
        assertThat(new String(record.headers().lastHeader(EVENT_TYPE_HEADER).value(),
            StandardCharsets.UTF_8)).isEqualTo(DOCUMENT_UPLOADED);
        JSONAssert.assertEquals("""
                {"documentId": "%s", "fileName": "contract.pdf", "fileSize": %d, "contentType": "application/pdf"}
                """.formatted(documentId, PDF_CONTENT.length),
            record.value(), JSONCompareMode.LENIENT);
    }

    @Test
    void shouldSendOutboxRowIdAsEventIdHeader() {
        //given
        var command = new UploadDocumentCommand(TEST_FILE_NAME, PDF_CONTENT.length, PDF.getMimeType(), source);

        //when
        var result = useCase.uploadDocument(command);

        //then
        UUID documentId = result.id().uuidValue();
        var record = awaitRecordFor(documentId);
        var outboxEventId = jdbcTemplate.queryForObject(QUERY_OUTBOX_EVENT_ID, UUID.class, documentId);
        var eventIdHeader = new String(record.headers().lastHeader(EVENT_ID_HEADER).value(),
            StandardCharsets.UTF_8);
        assertThat(UUID.fromString(eventIdHeader)).isEqualTo(outboxEventId);
    }

    @Test
    void shouldMarkOutboxRowPublished() {
        //given
        var command = new UploadDocumentCommand(TEST_FILE_NAME, PDF_CONTENT.length, PDF.getMimeType(), source);

        //when
        var result = useCase.uploadDocument(command);

        //then
        UUID documentId = result.id().uuidValue();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var publishedAt = jdbcTemplate.queryForObject(QUERY_OUTBOX_PUBLISHED_AT, Instant.class, documentId);
            assertThat(publishedAt).isNotNull();
        });
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }
}
