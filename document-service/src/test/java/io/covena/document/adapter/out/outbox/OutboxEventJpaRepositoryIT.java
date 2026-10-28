package io.covena.document.adapter.out.outbox;

import io.covena.document.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.NoSuchElementException;
import java.util.UUID;

import static io.covena.document.adapter.out.outbox.OutboxEventFixtures.T1;
import static io.covena.document.adapter.out.outbox.OutboxEventFixtures.T2;
import static io.covena.document.adapter.out.outbox.OutboxEventFixtures.T3;
import static io.covena.document.adapter.out.outbox.OutboxEventFixtures.eventCreatedAt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@Import({TestcontainersConfiguration.class})
class OutboxEventJpaRepositoryIT {
    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    private void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.clear();
    }

    private OutboxEventJpaEntity reload(UUID id) {
        return outboxEventJpaRepository
            .findById(id)
            .orElseThrow(() -> new NoSuchElementException("Outbox event " + id + " not found"));
    }

    @Test
    void shouldReturnOnlyUnpublishedEvents() {
        //given
        var published = eventCreatedAt(T1);
        published.markPublished(T1);
        testEntityManager.persist(published);
        var unpublishedOlder = eventCreatedAt(T2);
        testEntityManager.persist(unpublishedOlder);
        var unpublishedNewer = eventCreatedAt(T3);
        testEntityManager.persist(unpublishedNewer);
        flushAndClear();

        //when
        var events = outboxEventJpaRepository.lockNextUnpublished(4);

        //then
        assertThat(events)
            .extracting(OutboxEventJpaEntity::getId)
            .containsExactlyInAnyOrder(unpublishedOlder.getId(), unpublishedNewer.getId());
    }

    @Test
    void shouldReturnUnpublishedEventsOldestFirst() {
        //given
        var unpublishedNewest = eventCreatedAt(T3);
        testEntityManager.persist(unpublishedNewest);
        var unpublishedOldest = eventCreatedAt(T1);
        testEntityManager.persist(unpublishedOldest);
        var unpublishedOlder = eventCreatedAt(T2);
        testEntityManager.persist(unpublishedOlder);
        flushAndClear();

        //when
        var events = outboxEventJpaRepository.lockNextUnpublished(4);

        //then
        assertThat(events)
            .extracting(OutboxEventJpaEntity::getId)
            .containsExactly(unpublishedOldest.getId(), unpublishedOlder.getId(), unpublishedNewest.getId());
    }

    @Test
    void shouldReturnAtMostBatchSizeOldestEvents() {
        //given
        var unpublishedNewest = eventCreatedAt(T3);
        testEntityManager.persist(unpublishedNewest);
        var unpublishedOldest = eventCreatedAt(T1);
        testEntityManager.persist(unpublishedOldest);
        var unpublishedOlder = eventCreatedAt(T2);
        testEntityManager.persist(unpublishedOlder);
        flushAndClear();

        //when
        var events = outboxEventJpaRepository.lockNextUnpublished(2);

        //then
        assertThat(events)
            .extracting(OutboxEventJpaEntity::getId)
            .containsExactly(unpublishedOldest.getId(), unpublishedOlder.getId());
    }

    @Test
    void shouldReturnEmptyListWhenAllEventsArePublished() {
        //given
        var publishedOldest = eventCreatedAt(T1);
        publishedOldest.markPublished(T1);
        testEntityManager.persist(publishedOldest);
        var publishedOlder = eventCreatedAt(T2);
        publishedOlder.markPublished(T2);
        testEntityManager.persist(publishedOlder);
        var publishedNewest = eventCreatedAt(T3);
        publishedNewest.markPublished(T3);
        testEntityManager.persist(publishedNewest);
        flushAndClear();

        //when
        var events = outboxEventJpaRepository.lockNextUnpublished(4);

        //then
        assertThat(events).isEmpty();
    }

    @Test
    void shouldPersistPublishedAt() {
        //given
        var event = eventCreatedAt(T1);
        testEntityManager.persist(event);
        flushAndClear();

        //when
        var locked = outboxEventJpaRepository.lockNextUnpublished(1).getFirst();
        locked.markPublished(T3);
        flushAndClear();

        //then
        assertThat(reload(locked.getId()))
            .extracting(OutboxEventJpaEntity::getPublishedAt)
            .isEqualTo(T3);
    }
}
