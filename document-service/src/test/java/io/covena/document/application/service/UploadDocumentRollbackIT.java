package io.covena.document.application.service;

import io.covena.document.TestcontainersConfiguration;
import io.covena.document.application.port.in.ContentSource;
import io.covena.document.application.port.in.UploadDocumentCommand;
import io.covena.document.application.port.in.UploadDocumentUseCase;
import io.covena.document.application.port.in.UploadedDocumentResult;
import io.covena.document.application.port.out.DocumentEventOutbox;
import io.covena.document.application.port.out.DocumentRepository;
import io.covena.document.domain.Document;
import io.covena.document.domain.DocumentId;
import io.covena.document.domain.DocumentUploaded;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

import static io.covena.document.domain.ContentType.PDF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UploadDocumentRollbackIT {
    private static final byte[] PDF_CONTENT = "%PDF-1.7\nAcesta este un fișier de test".getBytes(StandardCharsets.UTF_8);
    private static final String TEST_FILE_NAME = "contract.pdf";
    private static final String QUERY_OUTBOX_EVENTS = "SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = ?";

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("covena.storage.local.base-dir", () -> tempDir.toString());
    }

    @MockitoSpyBean
    private DocumentRepository documentRepository;
    @MockitoSpyBean
    private DocumentEventOutbox eventOutbox;

    @Autowired
    UploadDocumentUseCase useCase;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ContentSource source;
    private final ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);

    @BeforeEach
    void setUp() {
        source = () -> new ByteArrayInputStream(PDF_CONTENT);
    }

    private Integer outboxRowsFor(UUID documentId) {
        return jdbcTemplate.queryForObject(QUERY_OUTBOX_EVENTS, Integer.class, documentId);
    }

    @Test
    void shouldKeepStoredContentWhenUploadCommits() {
        //given
        var command = new UploadDocumentCommand(TEST_FILE_NAME, PDF_CONTENT.length, PDF.getMimeType(), source);

        //when
        UploadedDocumentResult result = useCase.uploadDocument(command);

        //then
        DocumentId id = result.id();
        UUID uuid = id.uuidValue();
        var testFilePath = tempDir.resolve(uuid.toString());
        assertThat(documentRepository.findById(id)).get().extracting(Document::getId).isEqualTo(id);
        assertThat(testFilePath).hasBinaryContent(PDF_CONTENT);
        assertThat(outboxRowsFor(uuid)).isEqualTo(1);
        verify(eventOutbox).append(any(DocumentUploaded.class));
    }

    @Test
    void shouldDeleteStoredContentWhenUploadRollsBack() {
        //given
        var command = new UploadDocumentCommand(TEST_FILE_NAME, PDF_CONTENT.length, PDF.getMimeType(), source);
        doThrow(new OptimisticLockingFailureException("simulated database failure"))
            .when(documentRepository)
            .save(any(Document.class));

        //when/then
        assertThatThrownBy(() -> useCase.uploadDocument(command))
            .isInstanceOf(OptimisticLockingFailureException.class)
            .hasMessage("simulated database failure");

        //then
        verify(documentRepository).save(documentCaptor.capture());
        Document document = documentCaptor.getValue();
        DocumentId id = document.getId();
        UUID uuid = id.uuidValue();
        var testFilePath = tempDir.resolve(uuid.toString());
        assertThat(testFilePath).doesNotExist();
        assertThat(outboxRowsFor(uuid)).isZero();
        verify(eventOutbox, never()).append(any(DocumentUploaded.class));
    }

    @Test
    void shouldRollBackDocumentWhenOutboxAppendFails() {
        //given
        var command = new UploadDocumentCommand(TEST_FILE_NAME, PDF_CONTENT.length, PDF.getMimeType(), source);
        doThrow(new OptimisticLockingFailureException("simulated outbox database failure"))
            .when(eventOutbox)
            .append(any(DocumentUploaded.class));

        //when/then
        assertThatThrownBy(() -> useCase.uploadDocument(command))
            .isInstanceOf(OptimisticLockingFailureException.class)
            .hasMessage("simulated outbox database failure");

        //then
        verify(documentRepository).save(documentCaptor.capture());
        Document document = documentCaptor.getValue();
        DocumentId id = document.getId();
        UUID uuid = id.uuidValue();
        var testFilePath = tempDir.resolve(uuid.toString());
        assertThat(testFilePath).doesNotExist();
        assertThat(documentRepository.findById(id)).isEmpty();
        assertThat(outboxRowsFor(uuid)).isZero();
    }
}
