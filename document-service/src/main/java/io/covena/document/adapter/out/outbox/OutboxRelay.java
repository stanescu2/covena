package io.covena.document.adapter.out.outbox;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.covena.document.adapter.out.outbox.OutboxRelayConfiguration.DOCUMENT_EVENTS_TOPIC;

@ConditionalOnProperty(name = "covena.outbox.relay.enabled", havingValue = "true")
@Component
class OutboxRelay {
    private static final long SEND_TIMEOUT_SECONDS = 10;
    static final String EVENT_ID_HEADER = "eventId";
    static final String EVENT_TYPE_HEADER = "eventType";

    private final OutboxEventJpaRepository outboxEventRepository;
    private final KafkaOperations<String, String> kafkaOperations;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final OutboxRelayProperties outboxRelayProperties;

    OutboxRelay(OutboxEventJpaRepository outboxEventRepository,
                KafkaOperations<String, String> kafkaOperations,
                TransactionTemplate transactionTemplate,
                Clock clock,
                OutboxRelayProperties outboxRelayProperties) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaOperations = kafkaOperations;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.outboxRelayProperties = outboxRelayProperties;
    }

    @Scheduled(fixedDelayString = "${covena.outbox.relay.fixed-delay}")
    void publishPendingEvents() {
        transactionTemplate.executeWithoutResult(status -> {
            var events = outboxEventRepository.lockNextUnpublished(outboxRelayProperties.batchSize());
            for (OutboxEventJpaEntity event : events) {
                await(kafkaOperations.send(toRecord(event)));
                event.markPublished(clock.instant());
            }
        });
    }

    private void await(CompletableFuture<SendResult<String, String>> future) {
        try {
            future.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OutboxPublishException("Interrupted while publishing outbox events", e);
        } catch (ExecutionException e) {
            throw new OutboxPublishException("Kafka rejected an outbox event", e.getCause());
        } catch (TimeoutException e) {
            throw new OutboxPublishException("Kafka did not confirm outbox events in time", e);
        }
    }

    private static ProducerRecord<String, String> toRecord(OutboxEventJpaEntity entity) {
        var producerRecord = new ProducerRecord<>(DOCUMENT_EVENTS_TOPIC,
            entity.getAggregateId().toString(),
            entity.getPayload());
        producerRecord.headers().add(EVENT_ID_HEADER, entity.getId().toString().getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add(EVENT_TYPE_HEADER, entity.getEventType().getBytes(StandardCharsets.UTF_8));
        return producerRecord;
    }
}
