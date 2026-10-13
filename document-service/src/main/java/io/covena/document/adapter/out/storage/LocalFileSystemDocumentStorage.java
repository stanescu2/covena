package io.covena.document.adapter.out.storage;

import io.covena.document.application.port.out.DocumentContentStorage;
import io.covena.document.application.port.out.DocumentStorageException;
import io.covena.document.config.StorageProperties;
import io.covena.document.domain.DocumentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LocalFileSystemDocumentStorage implements DocumentContentStorage {
    private static final String CREATE_ERROR_MESSAGE_TEMPLATE = "Failed to create base directory %s for document storage";
    private static final String STORE_ERROR_MESSAGE_TEMPLATE = "Exception while storing document content with id %s";
    private static final String STORE_SUCCESS_LOG_MESSAGE = "Document content with id {} stored at {}";
    private static final String DELETE_SUCCESS_LOG_MESSAGE = "Document content with id {} deleted from {}";
    private static final String DELETE_ERROR_MESSAGE_TEMPLATE = "Exception while deleting document storage with id %s";

    private static final Logger logger = LoggerFactory.getLogger(LocalFileSystemDocumentStorage.class);

    private final Path baseDir;

    public LocalFileSystemDocumentStorage(StorageProperties properties) {
        this.baseDir = properties.baseDir();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException(CREATE_ERROR_MESSAGE_TEMPLATE.formatted(baseDir), e);
        }
    }

    @Override
    public void store(DocumentId id, InputStream fileStream) {
        String uuid = id.uuidValue().toString();
        Path path = baseDir.resolve(uuid);
        try {
            Files.copy(fileStream, path);
            logger.debug(STORE_SUCCESS_LOG_MESSAGE, uuid, path);
        } catch (IOException e) {
            throw new DocumentStorageException(STORE_ERROR_MESSAGE_TEMPLATE.formatted(uuid), e);
        }
    }

    @Override
    public void delete(DocumentId id) {
        String uuid = id.uuidValue().toString();
        Path path = baseDir.resolve(uuid);
        try {
            Files.deleteIfExists(path);
            logger.debug(DELETE_SUCCESS_LOG_MESSAGE, uuid, path);
        } catch (IOException e) {
            throw new DocumentStorageException(DELETE_ERROR_MESSAGE_TEMPLATE.formatted(uuid), e);
        }
    }
}
