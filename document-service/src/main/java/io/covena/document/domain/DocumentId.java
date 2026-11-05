package io.covena.document.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record DocumentId(UUID uuidValue) implements Serializable {
    public DocumentId {
        Objects.requireNonNull(uuidValue, "UUID should not be null");
    }

    public static DocumentId newId() {
        return new DocumentId(UUID.randomUUID());
    }
}
