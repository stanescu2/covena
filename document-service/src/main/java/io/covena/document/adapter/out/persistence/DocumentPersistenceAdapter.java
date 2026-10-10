package io.covena.document.adapter.out.persistence;

import io.covena.document.application.port.out.DocumentRepository;
import io.covena.document.domain.Document;
import io.covena.document.domain.DocumentId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class DocumentPersistenceAdapter implements DocumentRepository {
    private final DocumentMapper mapper;

    private final DocumentJpaRepository repository;

    DocumentPersistenceAdapter(DocumentJpaRepository repository, DocumentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Document> findById(DocumentId id) {
        return repository.findById(id.uuidValue()).map(mapper::toDocument);
    }

    @Override
    public void save(Document document) {
        DocumentJpaEntity entity = repository
            .findById(document.getId().uuidValue())
            .orElseGet(() -> mapper.toEntity(document));
        entity.updateStatus(document.getStatus(), document.getFailureReason());
        repository.save(entity);
    }
}
