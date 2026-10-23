package io.covena.document.adapter.out.outbox;

import io.covena.document.TestcontainersConfiguration;
import io.covena.document.domain.DocumentId;
import io.covena.document.domain.DocumentUploaded;
import io.covena.document.domain.FileName;
import io.covena.document.domain.FileSize;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static io.covena.document.domain.ContentType.PDF;
import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest(showSql = false)
@Import({TestcontainersConfiguration.class, JpaDocumentEventOutbox.class, JpaDocumentEventOutboxIT.FixedClockConfig.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class JpaDocumentEventOutboxIT {
    private static final Instant UPLOADED_AT = Instant.parse("2026-10-07T09:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2024-01-01T10:00:00.123456Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2024-01-01T10:00:00.123456789Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private JsonMapper mapper;

    @Autowired
    private JpaDocumentEventOutbox jpaDocumentEventOutbox;

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private OutboxEventJpaRepository repository;

    private static DocumentUploaded newDocumentUploaded() {
        return new DocumentUploaded(DocumentId.newId(),
            new FileName("contract.pdf"),
            new FileSize(1024),
            PDF,
            UPLOADED_AT);
    }

    private static DocumentUploadedPayload expectedPayload(String id) {
        return new DocumentUploadedPayload(id,
            "contract.pdf",
            1024,
            "application/pdf",
            UPLOADED_AT);
    }


    @Test
    void shouldStoreUnpublishedRowForDocumentUploaded() {
        var documentUploaded = newDocumentUploaded();
        jpaDocumentEventOutbox.append(documentUploaded);
        testEntityManager.flush();
        testEntityManager.clear();

        var all = repository.findAll();
        assertThat(all).hasSize(1);
        var storedEvent = all.getFirst();
        assertThat(storedEvent)
            .extracting(OutboxEventJpaEntity::getAggregateId,
                OutboxEventJpaEntity::getAggregateType,
                OutboxEventJpaEntity::getEventType,
                OutboxEventJpaEntity::getCreatedAt,
                OutboxEventJpaEntity::getPublishedAt)
            .containsExactly(documentUploaded.id().uuidValue(),
                "Document",
                "DocumentUploaded",
                CREATED_AT,
                null);
    }

    @Test
    void shouldGiveEachEventItsOwnId() {
        var documentUploaded = newDocumentUploaded();
        var documentId = documentUploaded.id().uuidValue();

        jpaDocumentEventOutbox.append(documentUploaded);
        jpaDocumentEventOutbox.append(documentUploaded);
        testEntityManager.flush();
        testEntityManager.clear();

        var all = repository.findAll();
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getId()).isNotEqualTo(all.get(1).getId());
        assertThat(all).extracting(OutboxEventJpaEntity::getAggregateId).containsOnly(documentId);
        assertThat(all).extracting(OutboxEventJpaEntity::getId).doesNotContain(documentId);
    }

    @Test
    void shouldStorePayloadAsIntegrationContract() {
        var documentUploaded = newDocumentUploaded();
        jpaDocumentEventOutbox.append(documentUploaded);
        testEntityManager.flush();
        testEntityManager.clear();
        DocumentUploadedPayload expectedPayload = expectedPayload(documentUploaded.id().uuidValue().toString());

        var all = repository.findAll();
        assertThat(all).hasSize(1);
        var storedEvent = all.getFirst();
        var storedPayloadJson = storedEvent.getPayload();
        var storedPayload = mapper.readValue(storedPayloadJson, DocumentUploadedPayload.class);
        assertThat(storedPayload).isEqualTo(expectedPayload);
        assertThat(storedPayloadJson).contains("2026-10-07T09:00:00Z");
    }
}
