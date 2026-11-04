package io.covena.document.adapter.out.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

@ConditionalOnProperty(name = "covena.outbox.cleanup.enabled", havingValue = "true")
@Component
class OutboxCleanup {
    private static final Logger logger = LoggerFactory.getLogger(OutboxCleanup.class);
    private final OutboxEventJpaRepository outboxEventRepository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final OutboxCleanupProperties outboxCleanupProperties;

    OutboxCleanup(OutboxEventJpaRepository outboxEventRepository,
                  TransactionTemplate transactionTemplate,
                  Clock clock,
                  OutboxCleanupProperties outboxCleanupProperties) {
        this.outboxEventRepository = outboxEventRepository;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.outboxCleanupProperties = outboxCleanupProperties;
    }

    @Scheduled(cron = "${covena.outbox.cleanup.cron}")
    void deleteExpiredEvents() {
        var threshold = clock.instant().minus(outboxCleanupProperties.retention());
        Integer deleted = transactionTemplate.execute(status -> outboxEventRepository.deletePublishedBefore(threshold));
        if (deleted != null && deleted > 0) {
            logger.info("Deleted {} published outbox events older than {}", deleted, threshold);
        }
    }
}
