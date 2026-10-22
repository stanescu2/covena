package io.covena.document.domain;

public record FileName(String name) {
    public static final int MAX_LENGTH = 255;

    public FileName {
        if (name == null || name.isBlank()) {
            throw new InvalidDocumentException("Name cannot be null or empty");
        }
        if (name.length() > MAX_LENGTH) {
            throw new InvalidDocumentException("Name cannot be longer than " + MAX_LENGTH + " characters");
        }
        if (name.contains("/") || name.contains("\\")) {
            throw new InvalidDocumentException("Name cannot contain '/' or '\\' characters");
        }
    }
}
