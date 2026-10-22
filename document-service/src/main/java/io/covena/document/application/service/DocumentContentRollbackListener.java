package io.covena.document.application.service;

import io.covena.document.application.port.out.DocumentContentStorage;
import io.covena.document.domain.DocumentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DocumentContentRollbackListener {
    private static final Logger logger = LoggerFactory.getLogger(DocumentContentRollbackListener.class);
    private static final String DOCUMENT_CONTENT_ROLLED_BACK_LOG_MESSAGE = "Upload rolled back, deleting stored content for document {}";

    private final DocumentContentStorage storage;

    public DocumentContentRollbackListener(DocumentContentStorage storage) {
        this.storage = storage;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void deleteContentAfterRollback(DocumentContentStored documentContentStored) {
        DocumentId documentId = documentContentStored.id();
        logger.warn(DOCUMENT_CONTENT_ROLLED_BACK_LOG_MESSAGE, documentId.uuidValue());
        storage.delete(documentId);
    }
}
