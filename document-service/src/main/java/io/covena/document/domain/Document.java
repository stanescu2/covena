package io.covena.document.domain;

import java.time.Instant;
import java.util.Objects;

public class Document {
  private final DocumentId id = DocumentId.newId();
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
    return new Document(fileName, contentType, fileSize, uploadedAt);
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
      throw new InvalidDocumentStatusException(getId(), status, required);
    }
  }

  public void markReady() {
    requireStatus(DocumentStatus.PROCESSING);
    status = DocumentStatus.READY;
  }

  public void markFailed(String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("reason must not be null or empty");
    }
    requireStatus(DocumentStatus.PROCESSING);
    status = DocumentStatus.FAILED;
    failureReason = reason;
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
