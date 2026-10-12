package io.covena.document.adapter.out.persistence;

import io.covena.document.domain.Document;
import io.covena.document.domain.DocumentId;
import io.covena.document.domain.FileName;
import io.covena.document.domain.FileSize;
import org.springframework.stereotype.Component;

@Component
class DocumentMapper {
    DocumentJpaEntity toEntity(Document document) {
        return new DocumentJpaEntity(document.getId().uuidValue(),
            document.getFileName().name(),
            document.getContentType(),
            document.getFileSize().fileSizeBytes(),
            document.getUploadedAt(),
            document.getStatus(),
            document.getFailureReason());
    }

    Document toDocument(DocumentJpaEntity documentJpaEntity) {
        return Document.restore(new DocumentId(documentJpaEntity.getId()),
            new FileName(documentJpaEntity.getFileName()),
            documentJpaEntity.getContentType(),
            new FileSize(documentJpaEntity.getFileSize()),
            documentJpaEntity.getUploadedAt(),
            documentJpaEntity.getStatus(),
            documentJpaEntity.getFailureReason());
    }
}
