package io.covena.document.domain;

import java.time.Instant;
import java.util.Objects;

public record DocumentUploaded(DocumentId id, FileName fileName, FileSize fileSize, ContentType contentType,
                               Instant uploadedAt) {
    public DocumentUploaded {
        Objects.requireNonNull(id, "Document id should not be null");
        Objects.requireNonNull(fileName, "File name should not be null");
        Objects.requireNonNull(fileSize, "File size should not be null");
        Objects.requireNonNull(contentType, "Content type should not be null");
        Objects.requireNonNull(uploadedAt, "Uploaded at should not be null");
    }

    public static DocumentUploaded from(Document document) {
        return new DocumentUploaded(document.getId(),
            document.getFileName(),
            document.getFileSize(),
            document.getContentType(),
            document.getUploadedAt());
    }
}
