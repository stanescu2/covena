package io.covena.document.application.service;

import io.covena.document.application.port.in.UploadDocumentCommand;
import io.covena.document.application.port.in.UploadDocumentUseCase;
import io.covena.document.application.port.in.UploadedDocumentResult;
import io.covena.document.application.port.out.DocumentContentStorage;
import io.covena.document.application.port.out.DocumentRepository;
import io.covena.document.domain.ContentType;
import io.covena.document.domain.Document;
import io.covena.document.domain.FileName;
import io.covena.document.domain.FileSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class UploadDocumentService implements UploadDocumentUseCase {
    private static final Logger logger = LoggerFactory.getLogger(UploadDocumentService.class);
    private static final String UPLOAD_COMMAND_RECEIVED_LOG_MESSAGE = "Upload document command received: {}";
    private static final String DOCUMENT_UPLOADED_LOG_MESSAGE = "Document uploaded: {}";

    private final Clock clock;
    private final DocumentContentStorage storage;
    private final DocumentRepository documentRepository;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public UploadDocumentService(DocumentContentStorage storage,
                                 DocumentRepository documentRepository,
                                 Clock clock,
                                 TransactionTemplate transactionTemplate,
                                 ApplicationEventPublisher eventPublisher) {
        this.clock = clock;
        this.storage = storage;
        this.documentRepository = documentRepository;
        this.transactionTemplate = transactionTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public UploadedDocumentResult uploadDocument(UploadDocumentCommand command) {
        logger.debug(UPLOAD_COMMAND_RECEIVED_LOG_MESSAGE, command);

        Document document = Document.upload(new FileName(command.fileName()),
            ContentType.fromMimeType(command.mimeType()),
            new FileSize(command.fileSize()),
            Instant.now(clock).truncatedTo(ChronoUnit.MICROS));
        storage.store(document.getId(), command.fileStream());
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new DocumentContentStored(document.getId()));
            documentRepository.save(document);
        });
        logger.debug(DOCUMENT_UPLOADED_LOG_MESSAGE, document.getId().uuidValue());
        return UploadedDocumentResult.from(document);
    }
}
