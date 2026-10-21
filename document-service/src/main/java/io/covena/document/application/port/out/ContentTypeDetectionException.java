package io.covena.document.application.port.out;

import java.io.Serial;

public class ContentTypeDetectionException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ContentTypeDetectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
