package io.covena.document.adapter.out.persistence;

import io.covena.document.domain.ContentType;
import io.covena.document.domain.DocumentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class DocumentJpaEntity {
    @Id
    private UUID id;

    private String fileName;

    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    private long fileSize;

    private Instant uploadedAt;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    private String failureReason;

    @Version
    private Long version;

    protected DocumentJpaEntity() {
    }

    protected DocumentJpaEntity(UUID id,
                                String fileName,
                                ContentType contentType,
                                long fileSize,
                                Instant uploadedAt,
                                DocumentStatus status,
                                String failureReason) {
        this.id = id;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
        this.status = status;
        this.failureReason = failureReason;
    }

    public String getFileName() {
        return fileName;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public UUID getId() {
        return id;
    }

    public void updateStatus(DocumentStatus status, String failureReason) {
        this.status = status;
        this.failureReason = failureReason;
    }

    public Long getVersion() {
        return version;
    }
}
