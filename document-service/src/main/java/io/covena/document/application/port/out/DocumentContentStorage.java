package io.covena.document.application.port.out;

import io.covena.document.domain.DocumentId;

import java.io.InputStream;

public interface DocumentContentStorage {
    void store(DocumentId documentId, InputStream fileStream);

    void delete(DocumentId documentId);
}
