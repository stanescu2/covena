package io.covena.document.adapter.out.persistence;

import io.covena.document.application.port.out.DocumentRepository;
import io.covena.document.domain.Document;
import io.covena.document.domain.FileName;
import io.covena.document.domain.FileSize;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Optional;

import static io.covena.document.domain.ContentType.PDF;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@Testcontainers
@Import({DocumentMapper.class, DocumentPersistenceAdapter.class})
class DocumentPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer(DockerImageName
        .parse("pgvector/pgvector:0.8.7-pg18-trixie")
        .asCompatibleSubstituteFor("postgres"));

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    void shouldSaveAndFindNewDocument() {
        Instant uploadedAt = Instant.parse("2026-10-10T10:15:30.123456Z");
        Document document = Document.upload(new FileName("contract.pdf"), PDF, new FileSize(1024), uploadedAt);

        documentRepository.save(document);
        testEntityManager.flush();
        testEntityManager.clear();
        Optional<Document> savedDocument = documentRepository.findById(document.getId());

        assertThat(savedDocument).isPresent();
        assertThat(savedDocument.get()).usingRecursiveComparison().isEqualTo(document);
    }

    @Test
    void shouldReturnEmptyWhenDocumentDoesNotExist() {
    }

    @Test
    void shouldUpdateStatusOfExistingDocument() {
    }

    @Test
    void shouldPersistFailureReasonForFailedDocument() {
    }

    @Test
    void shouldClearFailureReasonAfterRetry() {
    }

    @Test
    void shouldIncrementVersionOnUpdate() {
    }

    @Test
    void shouldKeepSingleRowAfterMultipleSaves() {
    }

    @Test
    void shouldRejectStaleUpdate() {
    }
}
