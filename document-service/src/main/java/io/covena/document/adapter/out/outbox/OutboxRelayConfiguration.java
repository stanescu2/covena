package io.covena.document.adapter.out.outbox;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(OutboxRelayProperties.class)
@EnableScheduling
class OutboxRelayConfiguration {
    static final String DOCUMENT_EVENTS_TOPIC = "covena.document-events";

    @Bean
    NewTopic documentEventsTopic() {
        return TopicBuilder.name(DOCUMENT_EVENTS_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }
}
