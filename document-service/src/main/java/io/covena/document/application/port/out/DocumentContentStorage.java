package io.covena.document.application.port.out;

import io.covena.document.application.port.in.ContentSource;
import io.covena.document.domain.DocumentId;

public interface DocumentContentStorage {
    void store(DocumentId documentId, ContentSource content);

    void delete(DocumentId documentId);
}
