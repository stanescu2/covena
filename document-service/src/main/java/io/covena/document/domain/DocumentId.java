package io.covena.document.domain;

import java.io.Serializable;
import java.util.UUID;

public record DocumentId(UUID uuidValue) implements Serializable {
  public DocumentId {
    if (uuidValue == null) {
      throw new IllegalArgumentException("UUID cannot be null");
    }
  }

  public static DocumentId newId() {
    return new DocumentId(UUID.randomUUID());
  }
}
