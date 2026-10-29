package io.covena.document.adapter.out.outbox;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static io.covena.document.adapter.out.outbox.OutboxEventFixtures.DOCUMENT_UPLOADED;
import static io.covena.document.adapter.out.outbox.OutboxEventFixtures.T1;
import static io.covena.document.adapter.out.outbox.OutboxEventFixtures.eventCreatedAt;
import static io.covena.document.adapter.out.outbox.OutboxRelay.EVENT_ID_HEADER;
import static io.covena.document.adapter.out.outbox.OutboxRelay.EVENT_TYPE_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {
    private static final Instant NOW = Instant.parse("2026-10-14T10:00:00Z");

    private final TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));
    private final OutboxRelayProperties properties =
        new OutboxRelayProperties(true, Duration.ofSeconds(1), 50);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private OutboxEventJpaRepository outboxEventRepository;
    @Mock
    private KafkaOperations<String, String> kafkaOperations;

    private OutboxRelay outboxRelay;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, String>> producerRecordCaptor;

    @BeforeEach
    void setUp() {
        outboxRelay = new OutboxRelay(outboxEventRepository, kafkaOperations,
            transactionTemplate, clock, properties);
    }

    private void givenLockedEvents(List<OutboxEventJpaEntity> events) {
        when(outboxEventRepository.lockNextUnpublished(anyInt())).thenReturn(events);
    }

    private void givenKafkaAcceptsEverything() {
        when(kafkaOperations.send(any(ProducerRecord.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void shouldRequestConfiguredBatchSize() {
        //when
        outboxRelay.publishPendingEvents();

        //then
        verify(outboxEventRepository).lockNextUnpublished(50);
    }

    @Test
    void shouldSendEventWithDocumentIdAsKey() {
        //given
        UUID documentId = UUID.randomUUID();
        var event = eventCreatedAt(documentId, T1);
        givenLockedEvents(List.of(event));
        givenKafkaAcceptsEverything();

        //when
        outboxRelay.publishPendingEvents();

        //then
        verify(kafkaOperations).send(producerRecordCaptor.capture());
        var producerRecord = producerRecordCaptor.getValue();
        assertThat(producerRecord.key()).isEqualTo(documentId.toString());
    }

    @Test
    void shouldAddEventIdAndEventTypeHeaders() {
        //given
        UUID eventId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        var event = eventCreatedAt(eventId, documentId, T1);
        givenLockedEvents(List.of(event));
        givenKafkaAcceptsEverything();

        //when
        outboxRelay.publishPendingEvents();

        //then
        verify(kafkaOperations).send(producerRecordCaptor.capture());
        var producerRecord = producerRecordCaptor.getValue();
        var expectedEventId = new RecordHeader(EVENT_ID_HEADER, eventId.toString().getBytes(StandardCharsets.UTF_8));
        var expectedEventType = new RecordHeader(EVENT_TYPE_HEADER, DOCUMENT_UPLOADED.getBytes(StandardCharsets.UTF_8));
        assertThat(producerRecord.headers()).containsExactlyInAnyOrder(expectedEventId, expectedEventType);
    }

    @Test
    void shouldMarkEventPublishedAfterKafkaConfirms() {
        //given
        var event = eventCreatedAt(T1);
        givenLockedEvents(List.of(event));
        givenKafkaAcceptsEverything();

        //when
        outboxRelay.publishPendingEvents();

        //then
        assertThat(event.getPublishedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldNotMarkEventWhenKafkaRejectsIt() {
        //given
        var event = eventCreatedAt(T1);
        givenLockedEvents(List.of(event));
        when(kafkaOperations.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.failedFuture(new KafkaException(
            "broker down")));

        //when/then
        assertThatThrownBy(() -> outboxRelay.publishPendingEvents())
            .isExactlyInstanceOf(OutboxPublishException.class)
            .hasMessage("Kafka rejected an outbox event")
            .hasCauseInstanceOf(KafkaException.class);
        assertThat(event.getPublishedAt()).isNull();
    }

    @Test
    void shouldSendNothingWhenNoEventsArePending() {
        //given
        givenLockedEvents(List.of());

        //when
        outboxRelay.publishPendingEvents();

        //then
        verifyNoInteractions(kafkaOperations);
    }
}
