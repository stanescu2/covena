package io.covena.document.domain;

import java.time.Instant;
import java.util.Objects;

public class Document {
    private DocumentId id;
    private final FileName fileName;
    private final FileSize fileSize;
    private final ContentType contentType;
    private DocumentStatus status = DocumentStatus.UPLOADED;
    private final Instant uploadedAt;
    private String failureReason;

    private Document(FileName fileName, ContentType contentType, FileSize fileSize, Instant uploadedAt) {
        this.fileName = Objects.requireNonNull(fileName, "File name should not be null");
        this.contentType = Objects.requireNonNull(contentType, "Content type should not be null");
        this.fileSize = Objects.requireNonNull(fileSize, "File size should not be null");
        this.uploadedAt = Objects.requireNonNull(uploadedAt, "Uploaded at should not be null");
    }

    public static Document upload(FileName fileName, ContentType contentType, FileSize fileSize, Instant uploadedAt) {
        Document document = new Document(fileName, contentType, fileSize, uploadedAt);
        document.id = DocumentId.newId();
        return document;
    }

    /**
     * Restores a document from persisted state.
     *
     * <p>This factory method reconstitutes an existing document with its original identifier,
     * upload metadata, status, and optional failure reason. Unlike
     * {@link #upload(FileName, ContentType, FileSize, Instant)}, it does not generate a new
     * document identifier.</p>
     *
     * <p>This method should be used by persistence
     * adapters, only for document reconstitution, within the document service.
     * This method is not part of the normal state-transition flow so it should not be invoked
     * outside the persistence adapter (adapter.out.persistence).
     * Otherwise, it may lead to unexpected behavior or errors.
     * To avoid this, this method is enforced by an ArchUnit rule.</p>
     *
     * @param id            the existing document identifier
     * @param fileName      the original file name
     * @param contentType   the document content type
     * @param fileSize      the document file size
     * @param uploadedAt    the instant when the document was uploaded
     * @param status        the current document status
     * @param failureReason the failure reason, or {@code null} if the document has no failure reason
     * @return the restored document
     */
    public static Document restore(DocumentId id,
                                   FileName fileName,
                                   ContentType contentType,
                                   FileSize fileSize,
                                   Instant uploadedAt,
                                   DocumentStatus status,
                                   String failureReason) {
        DocumentId documentId = Objects.requireNonNull(id, "Document id should not be null");
        DocumentStatus documentStatus = Objects.requireNonNull(status, "Document status should not be null");
        if (documentStatus == DocumentStatus.FAILED) {
            validateFailureReason(failureReason);
        } else if (failureReason != null) {
            throw new IllegalArgumentException(
                "For document status: " + documentStatus.name() + " the failure reason must be null");
        }
        Document document = new Document(fileName, contentType, fileSize, uploadedAt);
        document.id = documentId;
        document.status = documentStatus;
        document.failureReason = failureReason;
        return document;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public DocumentId getId() {
        return id;
    }

    public FileName getFileName() {
        return fileName;
    }

    public FileSize getFileSize() {
        return fileSize;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public boolean hasFailureReason() {
        return failureReason != null;
    }

    public void startProcessing() {
        requireStatus(DocumentStatus.UPLOADED);
        status = DocumentStatus.PROCESSING;
    }

    private void requireStatus(DocumentStatus required) {
        if (status != required) {
            throw new InvalidDocumentStatusException(getId(), required, status);
        }
    }

    public void markReady() {
        requireStatus(DocumentStatus.PROCESSING);
        status = DocumentStatus.READY;
    }

    public void markFailed(String reason) {
        validateFailureReason(reason);
        requireStatus(DocumentStatus.PROCESSING);
        status = DocumentStatus.FAILED;
        failureReason = reason;
    }

    private static void validateFailureReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Failure reason must not be null or blank");
        }
    }

    public void retry() {
        requireStatus(DocumentStatus.FAILED);
        status = DocumentStatus.PROCESSING;
        failureReason = null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(getId(), document.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
