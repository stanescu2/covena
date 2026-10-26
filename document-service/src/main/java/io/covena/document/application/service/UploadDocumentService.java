package io.covena.document.application.service;

import io.covena.document.application.port.in.UploadDocumentCommand;
import io.covena.document.application.port.in.UploadDocumentUseCase;
import io.covena.document.application.port.in.UploadedDocumentResult;
import io.covena.document.application.port.out.ContentTypeDetector;
import io.covena.document.application.port.out.DocumentContentStorage;
import io.covena.document.application.port.out.DocumentEventOutbox;
import io.covena.document.application.port.out.DocumentRepository;
import io.covena.document.domain.ContentType;
import io.covena.document.domain.Document;
import io.covena.document.domain.DocumentUploaded;
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
    private static final String MIME_TYPE_MISMATCH_LOG_MESSAGE = "Declared MIME type {} differs from detected {} for file {}";

    private final Clock clock;
    private final DocumentContentStorage storage;
    private final ContentTypeDetector detector;
    private final DocumentRepository documentRepository;
    private final DocumentEventOutbox eventOutbox;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public UploadDocumentService(DocumentContentStorage storage,
                                 ContentTypeDetector detector,
                                 DocumentRepository documentRepository,
                                 Clock clock,
                                 DocumentEventOutbox eventOutbox,
                                 TransactionTemplate transactionTemplate,
                                 ApplicationEventPublisher eventPublisher) {
        this.storage = storage;
        this.detector = detector;
        this.clock = clock;
        this.documentRepository = documentRepository;
        this.eventOutbox = eventOutbox;
        this.transactionTemplate = transactionTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public UploadedDocumentResult uploadDocument(UploadDocumentCommand command) {
        logger.debug(UPLOAD_COMMAND_RECEIVED_LOG_MESSAGE, command);
        var fileName = new FileName(command.fileName());
        var fileSize = new FileSize(command.fileSize());

        var detectedMimeType = detector.detectContentType(command.content(), command.fileName());
        if (!detectedMimeType.equalsIgnoreCase(command.declaredMimeType())) {
            logger.debug(MIME_TYPE_MISMATCH_LOG_MESSAGE,
                command.declaredMimeType(),
                detectedMimeType,
                command.fileName());
        }
        var contentType = ContentType.fromMimeType(detectedMimeType);
        var document = Document.upload(fileName,
            contentType,
            fileSize,
            Instant.now(clock).truncatedTo(ChronoUnit.MICROS));
        storage.store(document.getId(), command.content());

        var event = DocumentUploaded.from(document);
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new DocumentContentStored(document.getId()));
            documentRepository.save(document);
            eventOutbox.append(event);
        });
        logger.debug(DOCUMENT_UPLOADED_LOG_MESSAGE, document.getId().uuidValue());
        return UploadedDocumentResult.from(document);
    }
}
