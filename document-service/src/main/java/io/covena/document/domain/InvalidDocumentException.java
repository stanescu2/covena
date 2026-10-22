package io.covena.document.domain;

import java.io.Serial;

public class InvalidDocumentException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidDocumentException(String message) {
        super(message);
    }
}
