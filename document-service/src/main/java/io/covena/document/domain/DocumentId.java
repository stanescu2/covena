package io.covena.document.domain;

import java.util.UUID;

public record DocumentId(UUID uuidValue) {
  public DocumentId {
    if (uuidValue == null) {
      throw new IllegalArgumentException("UUID cannot be null");
    }
  }

  public static DocumentId newId() {
    return new DocumentId(UUID.randomUUID());
  }
}
