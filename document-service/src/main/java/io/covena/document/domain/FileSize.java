package io.covena.document.domain;

public record FileSize(long fileSizeBytes) {
    public static final long MAX_SIZE_BYTES = 50L * 1024 * 1024;

    public FileSize {
        if (fileSizeBytes == 0) {
            throw new InvalidDocumentException("File must not be empty");
        }
        if (fileSizeBytes < 0) {
            throw new InvalidDocumentException("File size cannot be negative");
        }
        if (fileSizeBytes > MAX_SIZE_BYTES) {
            throw new InvalidDocumentException("File size cannot exceed " + MAX_SIZE_BYTES + " bytes");
        }
    }
}
