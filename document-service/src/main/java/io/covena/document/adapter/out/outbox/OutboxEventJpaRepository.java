package io.covena.document.adapter.out.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {
    @Query(value = "SELECT * FROM outbox_events WHERE published_at IS NULL ORDER BY created_at LIMIT :batchSize FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEventJpaEntity> lockNextUnpublished(@Param("batchSize") int batchSize);

    @Modifying
    @Query("DELETE FROM OutboxEventJpaEntity WHERE publishedAt < :threshold")
    int deletePublishedBefore(@Param("threshold") Instant threshold);
}
