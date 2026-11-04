package io.covena.document.adapter.out.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class OutboxCleanupTest {
    private static final Instant NOW = Instant.parse("2026-10-14T10:00:00Z");
    private final TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));
    private final OutboxCleanupProperties outboxCleanupProperties = new OutboxCleanupProperties(true,
        Duration.ofDays(7),
        "0 0 0 * * ?");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private OutboxEventJpaRepository outboxEventRepository;

    private OutboxCleanup outboxCleanup;

    @BeforeEach
    void setUp() {
        outboxCleanup = new OutboxCleanup(outboxEventRepository,
            transactionTemplate,
            clock,
            outboxCleanupProperties);
    }

    private void givenDeletedCount(Integer deleted) {
        when(outboxEventRepository.deletePublishedBefore(any(Instant.class))).thenReturn(deleted);
    }

    @Test
    void shouldDeleteEventsPublishedBeforeRetention() {
        //when
        outboxCleanup.deleteExpiredEvents();

        //then
        verify(outboxEventRepository).deletePublishedBefore(Instant.parse("2026-10-07T10:00:00Z"));
    }

    @Test
    void shouldLogHowManyEventsWereDeleted(CapturedOutput outputCapture) {
        //given
        givenDeletedCount(3);

        //when
        outboxCleanup.deleteExpiredEvents();

        //then
        assertThat(outputCapture.getOut()).contains("Deleted 3 published outbox events");
    }

    @Test
    void shouldNotLogWhenNothingWasDeleted(CapturedOutput outputCapture) {
        //given
        givenDeletedCount(0);

        //when
        outboxCleanup.deleteExpiredEvents();

        //then
        assertThat(outputCapture.getOut()).doesNotContain("Deleted");
    }
}
