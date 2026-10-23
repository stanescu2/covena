package io.covena.document.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.covena.document.domain.ContentType.PDF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentUploadedTest {
    private static final Instant UPLOADED_AT = Instant.parse("2026-10-07T09:00:00Z");
    private static final DocumentId DOCUMENT_ID = DocumentId.newId();
    private static final FileName FILE_NAME = new FileName("contract.pdf");
    private static final FileSize FILE_SIZE = new FileSize(1024);

    private static Document newDocument() {
        return Document.upload(FILE_NAME, PDF, FILE_SIZE, UPLOADED_AT);
    }

    @Test
    void shouldCopyDocumentDataFromDocument() {
        var document = newDocument();
        DocumentUploaded documentUploaded = DocumentUploaded.from(document);

        assertThat(documentUploaded.id()).isEqualTo(document.getId());
        assertThat(documentUploaded.fileName()).isEqualTo(document.getFileName());
        assertThat(documentUploaded.fileSize()).isEqualTo(document.getFileSize());
        assertThat(documentUploaded.contentType()).isEqualTo(document.getContentType());
        assertThat(documentUploaded.uploadedAt()).isEqualTo(document.getUploadedAt());
    }

    @Test
    void shouldRejectNullDocumentId() {
        assertThatThrownBy(() -> new DocumentUploaded(null, FILE_NAME, FILE_SIZE, PDF, UPLOADED_AT))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Document id should not be null");
    }

    @Test
    void shouldRejectNullFileName() {
        assertThatThrownBy(() -> new DocumentUploaded(DOCUMENT_ID, null, FILE_SIZE, PDF, UPLOADED_AT))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("File name should not be null");
    }

    @Test
    void shouldRejectNullFileSize() {
        assertThatThrownBy(() -> new DocumentUploaded(DOCUMENT_ID, FILE_NAME, null, PDF, UPLOADED_AT))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("File size should not be null");
    }

    @Test
    void shouldRejectNullContentType() {
        assertThatThrownBy(() -> new DocumentUploaded(DOCUMENT_ID, FILE_NAME, FILE_SIZE, null, UPLOADED_AT))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Content type should not be null");
    }

    @Test
    void shouldRejectNullUploadedAt() {
        assertThatThrownBy(() -> new DocumentUploaded(DOCUMENT_ID, FILE_NAME, FILE_SIZE, PDF, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Uploaded at should not be null");
    }
}
