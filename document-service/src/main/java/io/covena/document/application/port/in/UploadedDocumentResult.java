package io.covena.document.application.port.in;

import io.covena.document.domain.Document;
import io.covena.document.domain.DocumentId;
import io.covena.document.domain.DocumentStatus;
import io.covena.document.domain.FileName;

import java.time.Instant;

public record UploadedDocumentResult(DocumentId id, FileName fileName, DocumentStatus status,
                                     Instant uploadedAt) {
    public static UploadedDocumentResult from(Document document) {
        return new UploadedDocumentResult(document.getId(),
            document.getFileName(),
            document.getStatus(),
            document.getUploadedAt());
    }
}
