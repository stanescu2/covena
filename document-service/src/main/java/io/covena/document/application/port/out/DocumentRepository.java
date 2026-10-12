package io.covena.document.application.port.out;

import io.covena.document.domain.Document;
import io.covena.document.domain.DocumentId;

import java.util.Optional;

public interface DocumentRepository {
    Optional<Document> findById(DocumentId id);

    void save(Document document);
}
