package io.covena.document.adapter.out.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {
}
