package io.covena.document.adapter.out.storage;

import io.covena.document.application.port.in.ContentSource;
import io.covena.document.application.port.out.DocumentStorageException;
import io.covena.document.config.StorageProperties;
import io.covena.document.domain.DocumentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileSystemDocumentStorageTest {
    private static final byte[] CONTENT = "Acesta este un fișier de test".getBytes(StandardCharsets.UTF_8);
    private static final byte[] OTHER_CONTENT = "Acesta este un alt fișier de test".getBytes(StandardCharsets.UTF_8);
    private LocalFileSystemDocumentStorage storage;
    private ContentSource source;
    @TempDir
    private Path tempDir;

    @BeforeEach
    void setup() {
        storage = new LocalFileSystemDocumentStorage(new StorageProperties(tempDir));
        source = () -> new ByteArrayInputStream(CONTENT);
    }

    @Test
    void shouldCreateBaseDirectoryOnStartup() {
        Path testDir = tempDir.resolve("testDir");
        assertThat(testDir).doesNotExist();
        new LocalFileSystemDocumentStorage(new StorageProperties(testDir));
        assertThat(testDir).isDirectory();
    }

    @Test
    void shouldStoreContentUnderDocumentId() {
        DocumentId documentId = DocumentId.newId();
        storage.store(documentId, source);
        Path documentPath = tempDir.resolve(documentId.uuidValue().toString());
        assertThat(documentPath).isRegularFile().hasBinaryContent(CONTENT);
    }

    @Test
    void shouldRejectStoringSameDocumentTwice() {
        DocumentId documentId = DocumentId.newId();
        Path documentPath = tempDir.resolve(documentId.uuidValue().toString());
        storage.store(documentId, source);
        assertThatThrownBy(() -> storage.store(documentId, () -> new ByteArrayInputStream(OTHER_CONTENT)))
            .isInstanceOf(DocumentStorageException.class)
            .hasCauseInstanceOf(FileAlreadyExistsException.class)
            .hasMessageContaining(documentId.uuidValue().toString());
        assertThat(documentPath).isRegularFile().hasBinaryContent(CONTENT);
    }

    @Test
    void shouldDeleteStoredContent() {
        DocumentId documentId = DocumentId.newId();
        Path documentPath = tempDir.resolve(documentId.uuidValue().toString());
        storage.store(documentId, source);
        assertThat(documentPath).isRegularFile().hasBinaryContent(CONTENT);
        storage.delete(documentId);
        assertThat(documentPath).doesNotExist();
    }

    @Test
    void shouldIgnoreDeleteOfMissingContent() {
        DocumentId documentId = DocumentId.newId();
        Path documentPath = tempDir.resolve(documentId.uuidValue().toString());
        assertThat(documentPath).doesNotExist();
        assertThatCode(() -> storage.delete(documentId)).doesNotThrowAnyException();
    }
}
