package io.covena.document.application.port.out;

import java.io.Serial;

public class DocumentStorageException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public DocumentStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
