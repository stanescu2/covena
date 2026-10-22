package io.covena.document.domain;

import java.io.Serial;

public class InvalidDocumentStatusException extends RuntimeException {
    private static final String MESSAGE_TEMPLATE = "For documentId %s expected document status %s but found %s";
    @Serial
    private static final long serialVersionUID = 1L;
    private final DocumentId documentId;
    private final DocumentStatus documentStatus;
    private final DocumentStatus expectedDocumentStatus;

    public InvalidDocumentStatusException(DocumentId documentId,
                                          DocumentStatus expectedDocumentStatus,
                                          DocumentStatus documentStatus) {
        super(MESSAGE_TEMPLATE.formatted(documentId.uuidValue(), expectedDocumentStatus, documentStatus));
        this.documentId = documentId;
        this.expectedDocumentStatus = expectedDocumentStatus;
        this.documentStatus = documentStatus;
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
