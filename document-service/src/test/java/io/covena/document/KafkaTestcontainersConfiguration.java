package io.covena.document;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;

@TestConfiguration(proxyBeanMethods = false)
public class KafkaTestcontainersConfiguration {
    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        // Keep in sync with the kafka image in infra/docker-compose.yml
        return new KafkaContainer("apache/kafka:4.3.1");
    }
}
