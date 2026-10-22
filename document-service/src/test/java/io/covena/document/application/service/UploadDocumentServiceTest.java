package io.covena.document.application.service;

import io.covena.document.application.port.in.ContentSource;
import io.covena.document.application.port.in.UploadDocumentCommand;
import io.covena.document.application.port.in.UploadedDocumentResult;
import io.covena.document.application.port.out.ContentTypeDetector;
import io.covena.document.application.port.out.DocumentContentStorage;
import io.covena.document.application.port.out.DocumentRepository;
import io.covena.document.application.port.out.DocumentStorageException;
import io.covena.document.domain.Document;
import io.covena.document.domain.DocumentId;
import io.covena.document.domain.DocumentStatus;
import io.covena.document.domain.FileName;
import io.covena.document.domain.InvalidDocumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static io.covena.document.domain.ContentType.PDF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadDocumentServiceTest {
    private static final byte[] CONTENT = "Acesta este un fișier de test".getBytes(StandardCharsets.UTF_8);
    private static final String TEST_FILE_NAME = "contract.pdf";
    private static final String TEXT_MIME_TYPE = "text/plain";
    private static final byte[] EMPTY_CONTENT = new byte[0];

    private static final Instant NOW = Instant.parse("2026-10-14T10:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private DocumentContentStorage storage;
    @Mock
    private ContentTypeDetector detector;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UploadDocumentService service;
    private final TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));
    private ContentSource source;

    @Captor
    private ArgumentCaptor<DocumentId> idCaptor;
    @Captor
    private ArgumentCaptor<Document> documentCaptor;
    @Captor
    private ArgumentCaptor<DocumentContentStored> eventCaptor;

    private void givenDetectedMimeType(String mimeType) {
        when(detector.detectContentType(source, TEST_FILE_NAME)).thenReturn(mimeType);
    }

    @BeforeEach
    void setUp() {
        service = new UploadDocumentService(storage,
            detector,
            documentRepository,
            clock,
            transactionTemplate,
            eventPublisher);
        source = () -> new ByteArrayInputStream(CONTENT);
    }

    @Test
    void shouldStoreContentAndSaveDocument() {
        //given
        var command = new UploadDocumentCommand(TEST_FILE_NAME, CONTENT.length, PDF.getMimeType(), source);
        givenDetectedMimeType(PDF.getMimeType());

        //when
        UploadedDocumentResult result = service.uploadDocument(command);

        // then: content stored under the document id
        verify(storage).store(idCaptor.capture(), eq(source));
        DocumentId documentId = idCaptor.getValue();

        // then: event published for the same document
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().id()).isEqualTo(documentId);

        // then: document saved with the same id and the expected fields
        verify(documentRepository).save(documentCaptor.capture());
        assertThat(documentCaptor.getValue())
            .extracting(Document::getId,
                Document::getFileName,
                Document::getContentType,
                Document::getStatus,
                Document::getUploadedAt)
            .containsExactly(documentId, new FileName(TEST_FILE_NAME), PDF, DocumentStatus.UPLOADED, NOW);

        // then: result describes the same document
        assertThat(result)
            .extracting(UploadedDocumentResult::id,
                UploadedDocumentResult::fileName,
                UploadedDocumentResult::status,
                UploadedDocumentResult::uploadedAt)
            .containsExactly(documentId, new FileName(TEST_FILE_NAME), DocumentStatus.UPLOADED, NOW);
    }

    @Test
    void shouldPublishEventBeforeSaving() {
        //given
        var command = new UploadDocumentCommand(TEST_FILE_NAME, CONTENT.length, PDF.getMimeType(), source);
        givenDetectedMimeType(PDF.getMimeType());

        //when
        service.uploadDocument(command);

        //then
        InOrder inOrder = Mockito.inOrder(storage, eventPublisher, documentRepository);
        inOrder.verify(storage).store(any(DocumentId.class), any(ContentSource.class));
        inOrder.verify(eventPublisher).publishEvent(any(DocumentContentStored.class));
        inOrder.verify(documentRepository).save(any(Document.class));
    }

    @Test
    void shouldNotSaveWhenStorageFails() {
        //given
        var command = new UploadDocumentCommand(TEST_FILE_NAME, CONTENT.length, PDF.getMimeType(), source);
        doThrow(new DocumentStorageException("storage error", null)).when(storage).store(any(), any());
        givenDetectedMimeType(PDF.getMimeType());

        //when/then
        assertThatThrownBy(() -> service.uploadDocument(command)).isExactlyInstanceOf(DocumentStorageException.class);
        verifyNoInteractions(eventPublisher, documentRepository);
    }

    @Test
    void shouldTruncateUploadTimeToMicros() {
        //given
        Instant testUploadedAt = Instant.parse("2026-10-14T10:00:00.123456789Z");
        var clockWithNanos = Clock.fixed(testUploadedAt, ZoneOffset.UTC);
        var command = new UploadDocumentCommand(TEST_FILE_NAME, CONTENT.length, PDF.getMimeType(), source);
        var serviceWithNanos = new UploadDocumentService(storage,
            detector,
            documentRepository,
            clockWithNanos,
            transactionTemplate,
            eventPublisher);
        givenDetectedMimeType(PDF.getMimeType());

        //when
        UploadedDocumentResult result = serviceWithNanos.uploadDocument(command);

        //then
        assertThat(result.uploadedAt()).isEqualTo(Instant.parse("2026-10-14T10:00:00.123456Z"));
    }

    @Test
    void shouldRejectFileWhoseContentIsNotThePdfItClaimsToBe() {
        //given
        var command = new UploadDocumentCommand(TEST_FILE_NAME, CONTENT.length, PDF.getMimeType(), source);
        givenDetectedMimeType(TEXT_MIME_TYPE);

        //when / then
        assertThatThrownBy(() -> service.uploadDocument(command))
            .isExactlyInstanceOf(InvalidDocumentException.class)
            .hasMessage("Unknown mime type: " + TEXT_MIME_TYPE);
        verifyNoInteractions(storage, eventPublisher, documentRepository);
    }

    @Test
    void shouldRejectEmptyFileBeforeDetectingContentType() {
        //given
        var command = new UploadDocumentCommand(TEST_FILE_NAME,
            EMPTY_CONTENT.length,
            PDF.getMimeType(),
            () -> new ByteArrayInputStream(EMPTY_CONTENT));

        //when / then
        assertThatThrownBy(() -> service.uploadDocument(command))
            .isExactlyInstanceOf(InvalidDocumentException.class)
            .hasMessage("File must not be empty");
        verifyNoInteractions(detector, storage, eventPublisher, documentRepository);
    }
}
