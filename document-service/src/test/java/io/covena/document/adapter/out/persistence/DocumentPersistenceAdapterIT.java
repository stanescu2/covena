package io.covena.document.adapter.out.persistence;

import io.covena.document.TestcontainersConfiguration;
import io.covena.document.application.port.out.DocumentRepository;
import io.covena.document.domain.Document;
import io.covena.document.domain.DocumentId;
import io.covena.document.domain.FileName;
import io.covena.document.domain.FileSize;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

import static io.covena.document.domain.ContentType.PDF;
import static io.covena.document.domain.DocumentStatus.FAILED;
import static io.covena.document.domain.DocumentStatus.PROCESSING;
import static io.covena.document.domain.DocumentStatus.READY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false)
@Import({DocumentMapper.class, DocumentPersistenceAdapter.class, TestcontainersConfiguration.class})
class DocumentPersistenceAdapterIT {
    private static final String UPLOADED_AT = "2026-10-10T10:15:30.123456Z";

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private DocumentJpaRepository jpaRepository;

    private static Document uploadDocument() {
        return Document.upload(new FileName("contract.pdf"), PDF, new FileSize(1024), Instant.parse(UPLOADED_AT));
    }

    private void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.clear();
    }

    private Document saveAndReload(Document document) {
        documentRepository.save(document);
        flushAndClear();
        return documentRepository
            .findById(document.getId())
            .orElseThrow(() -> new NoSuchElementException("Document " + document.getId() + " not found after save"));
    }

    private Long versionOf(Document document) {
        return jpaRepository.findById(document.getId().uuidValue()).orElseThrow().getVersion();
    }

    @Test
    void shouldSaveAndFindNewDocument() {
        Document document = uploadDocument();

        documentRepository.save(document);
        flushAndClear();
        Optional<Document> savedDocument = documentRepository.findById(document.getId());

        assertThat(savedDocument).isPresent();
        assertThat(savedDocument.get()).usingRecursiveComparison().isEqualTo(document);
    }

    @Test
    void shouldReturnEmptyWhenDocumentDoesNotExist() {
        Optional<Document> found = documentRepository.findById(DocumentId.newId());
        assertThat(found).isEmpty();
    }

    @Test
    void shouldUpdateStatusOfExistingDocument() {
        Document document = uploadDocument();

        Document loaded = saveAndReload(document);

        loaded.startProcessing();

        documentRepository.save(loaded);
        flushAndClear();
        Optional<Document> processing = documentRepository.findById(document.getId());

        assertThat(processing).isPresent();
        assertThat(processing.get()).usingRecursiveComparison().isEqualTo(loaded);
        assertThat(processing.get().getStatus()).isEqualTo(PROCESSING);
    }

    @Test
    void shouldPersistFailureReasonForFailedDocument() {
        Document document = uploadDocument();

        document.startProcessing();

        Document processing = saveAndReload(document);

        String failureReason = "PDF is password protected";
        processing.markFailed(failureReason);

        Document failed = saveAndReload(processing);

        assertThat(failed.getFailureReason()).isEqualTo(failureReason);
        assertThat(failed.getStatus()).isEqualTo(FAILED);
    }

    @Test
    void shouldClearFailureReasonAfterRetry() {
        Document document = uploadDocument();

        document.startProcessing();

        Document processing = saveAndReload(document);

        String failureReason = "PDF is password protected";
        processing.markFailed(failureReason);

        Document failed = saveAndReload(processing);

        failed.retry();

        Document retried = saveAndReload(failed);

        assertThat(retried.getFailureReason()).isNull();
        assertThat(retried.getStatus()).isEqualTo(PROCESSING);
    }

    @Test
    void shouldIncrementVersionOnUpdate() {
        Document document = uploadDocument();
        Document loaded = saveAndReload(document);
        long loadedVersion = versionOf(loaded);

        loaded.startProcessing();
        Document updated = saveAndReload(loaded);
        long updatedVersion = versionOf(updated);

        assertThat(loadedVersion).isZero();
        assertThat(updatedVersion).isEqualTo(1L);
    }

    @Test
    void shouldKeepSingleRowAfterMultipleSaves() {
        Document document = uploadDocument();
        Document loaded = saveAndReload(document);

        assertThat(jpaRepository.count()).isEqualTo(1L);

        loaded.startProcessing();
        Document processing = saveAndReload(loaded);

        assertThat(jpaRepository.count()).isEqualTo(1L);

        processing.markFailed("PDF wrong processing");
        Document failed = saveAndReload(processing);

        assertThat(jpaRepository.count()).isEqualTo(1L);

        failed.retry();
        Document retried = saveAndReload(failed);

        assertThat(jpaRepository.count()).isEqualTo(1L);

        retried.markReady();
        saveAndReload(retried);

        assertThat(jpaRepository.count()).isEqualTo(1L);
    }

    @Test
    void shouldRejectStaleUpdate() {
        Document document = uploadDocument();
        saveAndReload(document);

        DocumentJpaEntity entityUser2 = jpaRepository.findById(document.getId().uuidValue()).orElseThrow();
        testEntityManager.detach(entityUser2);
        assertThat(entityUser2.getVersion()).isZero();

        DocumentJpaEntity entityUser1 = jpaRepository.findById(document.getId().uuidValue()).orElseThrow();
        assertThat(entityUser1.getVersion()).isZero();

        entityUser1.updateStatus(PROCESSING, null);
        jpaRepository.save(entityUser1);
        flushAndClear();

        assertThat(entityUser1.getVersion()).isEqualTo(1);

        entityUser2.updateStatus(READY, null);
        assertThatThrownBy(() -> jpaRepository.save(entityUser2))
            .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
