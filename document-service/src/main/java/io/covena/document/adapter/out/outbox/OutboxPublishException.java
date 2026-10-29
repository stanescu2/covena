package io.covena.document.adapter.out.outbox;

class OutboxPublishException extends RuntimeException {
    OutboxPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
