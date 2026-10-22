package io.covena.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;

/**
 * Smoke test: the full application context starts against a fresh database
 * (all beans wired, configuration valid, Flyway migrations applied from scratch,
 * JPA entities validated against the migrated schema).
 */
@SpringBootTest
@Testcontainers
class DocumentServiceApplicationIT {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer(DockerImageName
        .parse("pgvector/pgvector:0.8.7-pg18-trixie")
        .asCompatibleSubstituteFor("postgres"));

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
