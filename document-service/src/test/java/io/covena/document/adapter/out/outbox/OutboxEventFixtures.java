package io.covena.document.adapter.out.outbox;

import java.time.Instant;
import java.util.UUID;

final class OutboxEventFixtures {
    static final String DOCUMENT = "Document";
    static final String DOCUMENT_UPLOADED = "DocumentUploaded";
    static final Instant T1 = Instant.parse("2024-01-01T10:00:00Z");
    static final Instant T2 = Instant.parse("2024-01-01T10:01:00Z");
    static final Instant T3 = Instant.parse("2024-01-01T10:02:00Z");

    private OutboxEventFixtures() {
    }

    static OutboxEventJpaEntity eventCreatedAt(Instant createdAt) {
        return new OutboxEventJpaEntity(UUID.randomUUID(),
            DOCUMENT,
            UUID.randomUUID(),
            DOCUMENT_UPLOADED,
            "{}",
            createdAt);
    }

    static OutboxEventJpaEntity eventCreatedAt(UUID aggregateId, Instant createdAt) {
        return new OutboxEventJpaEntity(UUID.randomUUID(),
            DOCUMENT,
            aggregateId,
            DOCUMENT_UPLOADED,
            "{}",
            createdAt);
    }

    static OutboxEventJpaEntity eventCreatedAt(UUID eventId, UUID aggregateId, Instant createdAt) {
        return new OutboxEventJpaEntity(eventId,
            DOCUMENT,
            aggregateId,
            DOCUMENT_UPLOADED,
            "{}",
            createdAt);
    }
}
