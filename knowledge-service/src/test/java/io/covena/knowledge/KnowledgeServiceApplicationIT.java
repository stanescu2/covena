package io.covena.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Smoke test: the full application context starts against a fresh database
 * (all beans wired, configuration valid, Flyway migrations applied from scratch).
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, KafkaTestcontainersConfiguration.class})
class KnowledgeServiceApplicationIT {

    @Test
    void contextLoads() {
    }
}
