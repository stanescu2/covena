package io.covena.document.adapter.out.outbox;

import io.covena.document.domain.DocumentUploaded;

import java.time.Instant;

record DocumentUploadedPayload(String documentId, String fileName, long fileSize, String contentType,
                               Instant uploadedAt) {
    public static DocumentUploadedPayload from(DocumentUploaded event) {
        return new DocumentUploadedPayload(event.id().uuidValue().toString(),
            event.fileName().name(),
            event.fileSize().fileSizeBytes(),
            event.contentType().getMimeType(),
            event.uploadedAt());
    }
}
