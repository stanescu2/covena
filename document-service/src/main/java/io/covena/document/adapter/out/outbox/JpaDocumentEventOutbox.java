package io.covena.document.adapter.out.outbox;

import io.covena.document.application.port.out.DocumentEventOutbox;
import io.covena.document.domain.DocumentUploaded;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
class JpaDocumentEventOutbox implements DocumentEventOutbox {
    private static final String DOCUMENT = "Document";
    private static final String DOCUMENT_UPLOADED = "DocumentUploaded";
    private final OutboxEventJpaRepository repository;
    private final Clock clock;
    private final JsonMapper mapper;

    public JpaDocumentEventOutbox(OutboxEventJpaRepository repository, Clock clock, JsonMapper mapper) {
        this.repository = repository;
        this.clock = clock;
        this.mapper = mapper;
    }

    @Override
    public void append(DocumentUploaded event) {
        var payload = DocumentUploadedPayload.from(event);

        String payloadJson = mapper.writeValueAsString(payload);

        OutboxEventJpaEntity outboxEvent = new OutboxEventJpaEntity(UUID.randomUUID(),
            DOCUMENT,
            event.id().uuidValue(),
            DOCUMENT_UPLOADED,
            payloadJson,
            Instant.now(clock).truncatedTo(ChronoUnit.MICROS));

        repository.save(outboxEvent);
    }
}
