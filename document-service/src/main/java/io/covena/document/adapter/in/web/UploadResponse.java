package io.covena.document.adapter.in.web;

import io.covena.document.application.port.in.UploadedDocumentResult;

import java.time.Instant;

public record UploadResponse(String id, String fileName, String status, Instant uploadedAt) {
    public static UploadResponse from(UploadedDocumentResult uploadedDocumentResult) {
        return new UploadResponse(uploadedDocumentResult.id().uuidValue().toString(),
            uploadedDocumentResult.fileName().name(),
            uploadedDocumentResult.status().name(),
            uploadedDocumentResult.uploadedAt());
    }
}
