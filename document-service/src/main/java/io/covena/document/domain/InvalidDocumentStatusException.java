package io.covena.document.domain;

import java.io.Serial;

public class InvalidDocumentStatusException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;
  private final DocumentId documentId;
  private final DocumentStatus documentStatus;
  private final DocumentStatus expectedDocumentStatus;

  public InvalidDocumentStatusException(DocumentId documentId, DocumentStatus documentStatus, DocumentStatus expectedDocumentStatus) {
    super("For documentId " + documentId + " expected document status " + expectedDocumentStatus + " but found " + documentStatus);
    this.documentId = documentId;
    this.documentStatus = documentStatus;
    this.expectedDocumentStatus = expectedDocumentStatus;
  }

  public DocumentId getDocumentId() {
    return documentId;
  }

  public DocumentStatus getDocumentStatus() {
    return documentStatus;
  }

  public DocumentStatus getExpectedDocumentStatus() {
    return expectedDocumentStatus;
  }
}
