package io.covena.document.adapter.in.web;

import io.covena.document.application.port.in.UploadDocumentCommand;
import io.covena.document.application.port.in.UploadDocumentUseCase;
import io.covena.document.application.port.in.UploadedDocumentResult;
import io.covena.document.application.port.out.DocumentStorageException;
import io.covena.document.domain.DocumentId;
import io.covena.document.domain.DocumentStatus;
import io.covena.document.domain.FileName;
import io.covena.document.domain.InvalidDocumentException;
import io.covena.document.domain.InvalidDocumentStatusException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static io.covena.document.domain.ContentType.PDF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@ExtendWith(OutputCaptureExtension.class)
class DocumentControllerTest {
    private static final byte[] CONTENT = "Acesta este un fișier de test".getBytes(StandardCharsets.UTF_8);
    private static final byte[] EMPTY_CONTENT = new byte[0];
    private static final String FILE_NAME = "TestPdfFile.pdf";
    private static final Instant NOW = Instant.parse("2026-10-14T10:00:00Z");

    @MockitoBean
    UploadDocumentUseCase useCase;

    @Autowired
    MockMvc mockMvc;

    @Test
    void shouldPassFileDataToUseCase() throws Exception {
        //given
        var captor = ArgumentCaptor.forClass(UploadDocumentCommand.class);
        var testPdfFile = new MockMultipartFile("file", FILE_NAME, PDF.getMimeType(), CONTENT);
        var result = new UploadedDocumentResult(DocumentId.newId(),
            new FileName(FILE_NAME),
            DocumentStatus.UPLOADED,
            NOW);
        when(useCase.uploadDocument(any())).thenReturn(result);

        //when
        mockMvc.perform(multipart("/api/documents").file(testPdfFile));

        //then
        verify(useCase).uploadDocument(captor.capture());
        assertThat(captor.getValue())
            .extracting(UploadDocumentCommand::fileName,
                UploadDocumentCommand::fileSize,
                UploadDocumentCommand::declaredMimeType)
            .containsExactly(FILE_NAME, (long) CONTENT.length, PDF.getMimeType());
    }

    @Test
    void shouldReturn201ForValidFile() throws Exception {
        //given
        var testPdfFile = new MockMultipartFile("file", FILE_NAME, PDF.getMimeType(), CONTENT);
        var result = new UploadedDocumentResult(DocumentId.newId(),
            new FileName(FILE_NAME),
            DocumentStatus.UPLOADED,
            NOW);
        var id = result.id().uuidValue().toString();
        when(useCase.uploadDocument(any())).thenReturn(result);

        //when/then
        mockMvc
            .perform(multipart("/api/documents").file(testPdfFile))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", endsWith("/api/documents/" + id)))
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.fileName").value(FILE_NAME))
            .andExpect(jsonPath("$.status").value("UPLOADED"))
            .andExpect(jsonPath("$.uploadedAt").value("2026-10-14T10:00:00Z"));
    }

    @Test
    void shouldReturn400WhenFilePartIsMissing() throws Exception {
        //given
        var wrongPart = new MockMultipartFile("doc", FILE_NAME, PDF.getMimeType(), CONTENT);

        //when/then
        mockMvc
            .perform(multipart("/api/documents").file(wrongPart))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(useCase);
    }

    @Test
    void shouldReturn500WithGenericDetailWhenUnexpectedErrorOccurs(CapturedOutput output) throws Exception {
        //given
        var testPdfFile = new MockMultipartFile("file", FILE_NAME, PDF.getMimeType(), CONTENT);
        when(useCase.uploadDocument(any())).thenThrow(new DocumentStorageException("disk full", null));

        //when/then
        mockMvc
            .perform(multipart("/api/documents").file(testPdfFile))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
        assertThat(output).contains("Unexpected error").contains("disk full");
    }

    @Test
    void shouldReturn400WithDetailWhenDocumentIsInvalid() throws Exception {
        //given
        var emptyTestPdfFile = new MockMultipartFile("file", FILE_NAME, PDF.getMimeType(), EMPTY_CONTENT);
        when(useCase.uploadDocument(any())).thenThrow(new InvalidDocumentException("File must not be empty"));

        //when/then
        mockMvc
            .perform(multipart("/api/documents").file(emptyTestPdfFile))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Invalid document"))
            .andExpect(jsonPath("$.detail").value("File must not be empty"));
    }

    @Test
    void shouldReturn409WhenDocumentStatusTransitionIsInvalid(CapturedOutput output) throws Exception {
        //given
        var testPdfFile = new MockMultipartFile("file", FILE_NAME, PDF.getMimeType(), CONTENT);
        var documentId = DocumentId.newId();
        when(useCase.uploadDocument(any())).thenThrow(new InvalidDocumentStatusException(documentId,
            DocumentStatus.UPLOADED,
            DocumentStatus.READY));

        //when/then
        mockMvc
            .perform(multipart("/api/documents").file(testPdfFile))
            .andExpect(status().isConflict())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.title").value("Invalid document status transition"))
            .andExpect(jsonPath("$.detail").value(
                "For documentId " + documentId.uuidValue() + " expected document status UPLOADED but found READY"))
            .andExpect(jsonPath("$.documentId").value(documentId.uuidValue().toString()));
        assertThat(output).contains("Invalid document status transition");
    }
}
