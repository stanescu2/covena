package io.covena.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

/**
 * Smoke test: the full application context starts against a fresh database
 * (all beans wired, configuration valid, Flyway migrations applied from scratch,
 * JPA entities validated against the migrated schema).
 */
@SpringBootTest(properties = "covena.outbox.relay.enabled=false")
@Import(TestcontainersConfiguration.class)
class DocumentServiceApplicationIT {
    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("covena.storage.local.base-dir", () -> tempDir.toString());
    }

    @Test
    void contextLoads() {
    }
}
