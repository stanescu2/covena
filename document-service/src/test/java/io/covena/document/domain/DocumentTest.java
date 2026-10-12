package io.covena.document.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;

import static io.covena.document.domain.DocumentStatus.FAILED;
import static io.covena.document.domain.DocumentStatus.PROCESSING;
import static io.covena.document.domain.DocumentStatus.READY;
import static io.covena.document.domain.DocumentStatus.UPLOADED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentTest {
    private static final Instant UPLOADED_AT = Instant.parse("2026-10-07T09:00:00Z");

    private static Document newDocument() {
        return Document.upload(new FileName("contract.pdf"), ContentType.PDF, new FileSize(1024), UPLOADED_AT);
    }

    private static Document processingDocument() {
        Document document = newDocument();
        document.startProcessing();
        return document;
    }

    private static Document restoreTestDocument(DocumentId documentId, DocumentStatus status, String failureReason) {
        return Document.restore(
            documentId,
            new FileName("contract.pdf"),
            ContentType.PDF,
            new FileSize(1024),
            UPLOADED_AT,
            status,
            failureReason);
    }

    @Test
    void shouldStartInUploadedStatusWithoutFailureReason() {
        Document document = newDocument();
        assertEquals(UPLOADED, document.getStatus());
        assertNull(document.getFailureReason());
    }

    @Test
    void shouldKeepUploadDataAsGiven() {
        FileName fileName = new FileName("contract.pdf");
        FileSize fileSize = new FileSize(1024);
        Instant uploadedAt = UPLOADED_AT;
        Document document = Document.upload(fileName, ContentType.PDF, fileSize, uploadedAt);
        assertEquals(fileName, document.getFileName());
        assertEquals(fileSize, document.getFileSize());
        assertEquals(uploadedAt, document.getUploadedAt());
        assertEquals(UPLOADED, document.getStatus());
        assertNull(document.getFailureReason());
        assertEquals(ContentType.PDF, document.getContentType());
    }

    @Test
    void shouldGenerateIdOnUpload() {
        Document document = newDocument();
        assertNotNull(document.getId());
    }

    @Test
    void shouldRejectNullArgumentsOnUpload() {
        List<Executable> testCases = List.of(() -> Document.upload(null,
                ContentType.PDF,
                new FileSize(1024),
                UPLOADED_AT),
            () -> Document.upload(new FileName("contract.pdf"), null, new FileSize(1024), UPLOADED_AT),
            () -> Document.upload(new FileName("contract.pdf"), ContentType.PDF, null, UPLOADED_AT),
            () -> Document.upload(new FileName("contract.pdf"), ContentType.PDF, new FileSize(1024), null));

        testCases.forEach(testExecute -> assertThrows(NullPointerException.class, testExecute));
    }

    @Test
    void shouldMoveToProcessingWhenStartProcessingFromUploaded() {
        Document document = processingDocument();
        assertEquals(PROCESSING, document.getStatus());
        assertNull(document.getFailureReason());
    }

    @Test
    void shouldMoveToReadyWhenMarkReadyFromProcessing() {
        Document document = processingDocument();
        document.markReady();
        assertEquals(READY, document.getStatus());
        assertNull(document.getFailureReason());
    }

    @Test
    void shouldMoveToFailedAndKeepReasonWhenMarkFailedFromProcessing() {
        Document document = processingDocument();
        document.markFailed("Test failure reason");
        assertEquals(FAILED, document.getStatus());
        assertEquals("Test failure reason", document.getFailureReason());
    }

    @Test
    void shouldMoveToProcessingAndClearReasonWhenRetryFromFailed() {
        Document document = processingDocument();
        document.markFailed("Test failure reason");
        document.retry();
        assertEquals(PROCESSING, document.getStatus());
        assertNull(document.getFailureReason());
    }

    @Test
    void shouldRejectStartProcessingWhenNotUploaded() {
        Document document = processingDocument();
        InvalidDocumentStatusException exception = assertThrows(InvalidDocumentStatusException.class,
            document::startProcessing);
        assertEquals(PROCESSING, exception.getDocumentStatus());
        assertEquals(UPLOADED, exception.getExpectedDocumentStatus());
        assertEquals(PROCESSING, document.getStatus());
    }

    @Test
    void shouldRejectMarkReadyWhenNotProcessing() {
        Document document = processingDocument();
        document.markFailed("Test failure reason");
        InvalidDocumentStatusException exception = assertThrows(InvalidDocumentStatusException.class,
            document::markReady);
        assertEquals(FAILED, exception.getDocumentStatus());
        assertEquals(PROCESSING, exception.getExpectedDocumentStatus());
        assertEquals(FAILED, document.getStatus());
    }

    @Test
    void shouldRejectMarkFailedWhenNotProcessing() {
        Document document = processingDocument();
        document.markFailed("Test failure reason");
        InvalidDocumentStatusException exception = assertThrows(InvalidDocumentStatusException.class,
            () -> document.markFailed("Test failure reason"));
        assertEquals(FAILED, exception.getDocumentStatus());
        assertEquals(PROCESSING, exception.getExpectedDocumentStatus());
        assertEquals(FAILED, document.getStatus());
    }

    @Test
    void shouldRejectRetryWhenNotFailed() {
        Document document = processingDocument();
        InvalidDocumentStatusException exception = assertThrows(InvalidDocumentStatusException.class, document::retry);
        assertEquals(PROCESSING, exception.getDocumentStatus());
        assertEquals(FAILED, exception.getExpectedDocumentStatus());
        assertEquals(PROCESSING, document.getStatus());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"\t", " "})
    void shouldRejectBlankFailureReasonAndKeepStatus(String failureReason) {
        Document document = processingDocument();
        assertThrows(IllegalArgumentException.class, () -> document.markFailed(failureReason));
        assertEquals(PROCESSING, document.getStatus());
    }

    @Test
    void shouldRestoreWithGivenIdAndStatus() {
        DocumentId documentId = DocumentId.newId();
        Document restored = restoreTestDocument(documentId, PROCESSING, null);
        assertEquals(documentId, restored.getId());
        assertEquals(new FileName("contract.pdf"), restored.getFileName());
        assertEquals(ContentType.PDF, restored.getContentType());
        assertEquals(new FileSize(1024), restored.getFileSize());
        assertEquals(UPLOADED_AT, restored.getUploadedAt());
        assertEquals(PROCESSING, restored.getStatus());
    }

    @Test
    void shouldAllowTransitionsAfterRestore() {
        DocumentId documentId = DocumentId.newId();
        Document restored = restoreTestDocument(documentId, PROCESSING, null);
        restored.markReady();
        assertEquals(READY, restored.getStatus());
        assertNull(restored.getFailureReason());
    }

    @Test
    void shouldRejectNullArgumentsOnRestore() {
        DocumentId documentId = DocumentId.newId();

        List<Executable> testCases = List.of(
            () -> Document.restore(
                null,
                new FileName("contract.pdf"),
                ContentType.PDF,
                new FileSize(1024),
                UPLOADED_AT,
                PROCESSING,
                null
            ),
            () -> Document.restore(
                documentId,
                null,
                ContentType.PDF,
                new FileSize(1024),
                UPLOADED_AT,
                PROCESSING,
                null
            ),
            () -> Document.restore(
                documentId,
                new FileName("contract.pdf"),
                null,
                new FileSize(1024),
                UPLOADED_AT,
                PROCESSING,
                null
            ),
            () -> Document.restore(
                documentId,
                new FileName("contract.pdf"),
                ContentType.PDF,
                null,
                UPLOADED_AT,
                PROCESSING,
                null
            ),
            () -> Document.restore(
                documentId,
                new FileName("contract.pdf"),
                ContentType.PDF,
                new FileSize(1024),
                null,
                PROCESSING,
                null
            ),
            () -> Document.restore(
                documentId,
                new FileName("contract.pdf"),
                ContentType.PDF,
                new FileSize(1024),
                UPLOADED_AT,
                null,
                null
            )
        );

        testCases.forEach(exec -> assertThrows(NullPointerException.class, exec));
    }

    @Test
    void shouldRestoreFailedDocumentWithReason() {
        DocumentId documentId = DocumentId.newId();
        Document restored = restoreTestDocument(documentId, FAILED, "error");
        assertEquals(documentId, restored.getId());
        assertEquals(new FileName("contract.pdf"), restored.getFileName());
        assertEquals(ContentType.PDF, restored.getContentType());
        assertEquals(new FileSize(1024), restored.getFileSize());
        assertEquals(UPLOADED_AT, restored.getUploadedAt());
        assertEquals(FAILED, restored.getStatus());
        assertEquals("error", restored.getFailureReason());
    }

    @Test
    void shouldAllowRetryAfterRestoringFailedDocument() {
        DocumentId documentId = DocumentId.newId();
        Document restored = restoreTestDocument(documentId, FAILED, "error");
        restored.retry();
        assertEquals(PROCESSING, restored.getStatus());
        assertNull(restored.getFailureReason());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void shouldRejectRestoringFailedDocumentWithoutReason(String failureReason) {
        Executable restoreCall = () -> restoreTestDocument(DocumentId.newId(), FAILED, failureReason);
        assertThrows(IllegalArgumentException.class, restoreCall);
    }

    @ParameterizedTest
    @EnumSource(value = DocumentStatus.class, names = {"FAILED"}, mode = EnumSource.Mode.EXCLUDE)
    void shouldRejectRestoringNonFailedDocumentWithReason(DocumentStatus status) {
        Executable restoreCall = () -> restoreTestDocument(DocumentId.newId(), status, "error");
        assertThrows(IllegalArgumentException.class, restoreCall);
    }

    @Test
    @SuppressWarnings("EqualsWithItself")
    void shouldBeEqualToItself() {
        Document document = newDocument();
        assertEquals(document, document);
    }

    @Test
    void shouldNotBeEqualToAnotherDocumentWithSameData() {
        Document document = newDocument();
        assertNotEquals(document, newDocument());
    }
}
