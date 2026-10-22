package io.covena.document;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(
            // Keep in sync with the postgres image in infra/docker-compose.yml
            DockerImageName.parse("pgvector/pgvector:0.8.7-pg18-trixie")
                .asCompatibleSubstituteFor("postgres")
        );
    }
}
