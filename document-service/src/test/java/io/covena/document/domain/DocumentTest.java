package io.covena.document.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class DocumentTest {
  private static final Instant UPLOADED_AT = Instant.parse("2026-10-07T09:00:00Z");

  private static Document newDocument() {
    return Document.upload(new FileName("contract.pdf"), ContentType.PDF, new FileSize(1024), UPLOADED_AT);
  }

  private static Document processingDocument() {
    Document document = newDocument();
    document.startProcessing();
    return document;
  }

  @Test
  void shouldStartInUploadedStatusWithoutFailureReason() {
    Document document = newDocument();
    assertEquals(DocumentStatus.UPLOADED, document.getStatus());
    assertNull(document.getFailureReason());
  }


  @Test
  void shouldKeepUploadDataAsGiven() {
    FileName fileName = new FileName("contract.pdf");
    FileSize fileSize = new FileSize(1024);
    Instant uploadedAt = UPLOADED_AT;
    Document document = Document.upload(fileName, ContentType.PDF, fileSize, uploadedAt);
    assertEquals(fileName, document.getFileName());
    assertEquals(fileSize, document.getFileSize());
    assertEquals(uploadedAt, document.getUploadedAt());
    assertEquals(DocumentStatus.UPLOADED, document.getStatus());
    assertNull(document.getFailureReason());
    assertEquals(ContentType.PDF, document.getContentType());
  }

  @Test
  void shouldGenerateIdOnUpload() {
    Document document = newDocument();
    assertNotNull(document.getId());
  }

  @Test
  void shouldRejectNullArgumentsOnUpload() {
    List<Executable> testCases = List.of(() -> Document.upload(null, ContentType.PDF, new FileSize(1024), UPLOADED_AT),
        () -> Document.upload(new FileName("contract.pdf"), null, new FileSize(1024), UPLOADED_AT),
        () -> Document.upload(new FileName("contract.pdf"), ContentType.PDF, null, UPLOADED_AT),
        () -> Document.upload(new FileName("contract.pdf"), ContentType.PDF, new FileSize(1024), null));

    testCases.forEach(testExecute -> assertThrows(NullPointerException.class, testExecute));
  }

  @Test
  void shouldMoveToProcessingWhenStartProcessingFromUploaded() {
    Document document = processingDocument();
    assertEquals(DocumentStatus.PROCESSING, document.getStatus());
    assertNull(document.getFailureReason());
  }

  @Test
  void shouldMoveToReadyWhenMarkReadyFromProcessing() {
    Document document = processingDocument();
    document.markReady();
    assertEquals(DocumentStatus.READY, document.getStatus());
    assertNull(document.getFailureReason());
  }

  @Test
  void shouldMoveToFailedAndKeepReasonWhenMarkFailedFromProcessing() {
    Document document = processingDocument();
    document.markFailed("Test failure reason");
    assertEquals(DocumentStatus.FAILED, document.getStatus());
    assertEquals("Test failure reason", document.getFailureReason());
  }

  @Test
  void shouldMoveToProcessingAndClearReasonWhenRetryFromFailed() {
    Document document = processingDocument();
    document.markFailed("Test failure reason");
    document.retry();
    assertEquals(DocumentStatus.PROCESSING, document.getStatus());
    assertNull(document.getFailureReason());
  }

  @Test
  void shouldRejectStartProcessingWhenNotUploaded() {
    Document document = processingDocument();
    InvalidDocumentStatusException exception = assertThrows(InvalidDocumentStatusException.class, document::startProcessing);
    assertEquals(DocumentStatus.PROCESSING, exception.getDocumentStatus());
    assertEquals(DocumentStatus.UPLOADED, exception.getExpectedDocumentStatus());
    assertEquals(DocumentStatus.PROCESSING, document.getStatus());
  }

  @Test
  void shouldRejectMarkReadyWhenNotProcessing() {
    Document document = processingDocument();
    document.markFailed("Test failure reason");
    InvalidDocumentStatusException exception = assertThrows(InvalidDocumentStatusException.class, document::markReady);
    assertEquals(DocumentStatus.FAILED, exception.getDocumentStatus());
    assertEquals(DocumentStatus.PROCESSING, exception.getExpectedDocumentStatus());
    assertEquals(DocumentStatus.FAILED, document.getStatus());
  }

  @Test
  void shouldRejectMarkFailedWhenNotProcessing() {
    Document document = processingDocument();
    document.markFailed("Test failure reason");
    InvalidDocumentStatusException exception = assertThrows(InvalidDocumentStatusException.class, () -> document.markFailed("Test failure reason"));
    assertEquals(DocumentStatus.FAILED, exception.getDocumentStatus());
    assertEquals(DocumentStatus.PROCESSING, exception.getExpectedDocumentStatus());
    assertEquals(DocumentStatus.FAILED, document.getStatus());
  }

  @Test
  void shouldRejectRetryWhenNotFailed() {
    Document document = processingDocument();
    InvalidDocumentStatusException exception = assertThrows(InvalidDocumentStatusException.class, document::retry);
    assertEquals(DocumentStatus.PROCESSING, exception.getDocumentStatus());
    assertEquals(DocumentStatus.FAILED, exception.getExpectedDocumentStatus());
    assertEquals(DocumentStatus.PROCESSING, document.getStatus());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"\t", " "})
  void shouldRejectBlankFailureReasonAndKeepStatus(String failureReason) {
    Document document = processingDocument();
    assertThrows(IllegalArgumentException.class, () -> document.markFailed(failureReason));
    assertEquals(DocumentStatus.PROCESSING, document.getStatus());
  }

  @Test
  void shouldBeEqualToItself() {
    Document document = newDocument();
    assertEquals(document, document);
  }

  @Test
  void shouldNotBeEqualToAnotherDocumentWithSameData() {
    Document document = newDocument();
    assertNotEquals(document, newDocument());
  }
}
