package io.covena.document.adapter.out.outbox;

import org.junit.jupiter.api.Test;

import static io.covena.document.adapter.out.outbox.OutboxEventFixtures.T1;
import static io.covena.document.adapter.out.outbox.OutboxEventFixtures.eventCreatedAt;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxEventJpaEntityTest {

    @Test
    void shouldRejectNullPublishedAt() {
        var event = eventCreatedAt(T1);
        assertThatThrownBy(() -> event.markPublished(null))
            .isExactlyInstanceOf(NullPointerException.class)
            .hasMessage("Published at should not be null");
    }
}
