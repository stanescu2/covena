package io.covena.document.domain;

import java.time.Instant;
import java.util.Objects;

public class Document {
  private final DocumentId id = DocumentId.newId();
  private final FileName fileName;
  private final FileSize fileSize;
  private final ContentType contentType;
  private DocumentStatus status = DocumentStatus.UPLOADED;
  private final Instant uploadedAt = Instant.now();
  private String failureReason;

  private Document(FileName fileName, ContentType contentType, FileSize fileSize) {
    this.fileName = Objects.requireNonNull(fileName, "File name should not be null");
    this.contentType = Objects.requireNonNull(contentType, "Content type should not be null");
    this.fileSize = Objects.requireNonNull(fileSize, "File size should not be null");
  }

  public static Document upload(FileName fileName, ContentType contentType, FileSize fileSize) {
    return new Document(fileName, contentType, fileSize);
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
}
