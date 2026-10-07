package io.covena.document.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentTypeTest {

  static Stream<Arguments> argumentsMimeTypes() {
    return Stream.of(
        Arguments.of(ContentType.PDF, "Application/pdf"),
        Arguments.of(ContentType.PDF, "Application/Pdf"),
        Arguments.of(ContentType.PDF, "application/Pdf"),
        Arguments.of(ContentType.PDF, "application/pdf"),
        Arguments.of(ContentType.DOCX, "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        Arguments.of(ContentType.DOCX, "APPLICATION/VND.OPENXMLFORMATS-OFFICEDOCUMENT.WORDPROCESSINGML.DOCUMENT")
    );
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"image/png", "text/plain", "application/msword", "application/pdf "})
  void shouldRejectInvalidMimeType(String invalidMimeType) {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ContentType.fromMimeType(invalidMimeType));
    assertEquals("Unknown mime type: " + invalidMimeType, exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("argumentsMimeTypes")
  void shouldMapMimeTypeToContentType(ContentType expected, String mimeType) {
    assertEquals(expected, ContentType.fromMimeType(mimeType));
  }

  @ParameterizedTest
  @CsvSource({
      "PDF, application/pdf",
      "DOCX, application/vnd.openxmlformats-officedocument.wordprocessingml.document"
  })
  void shouldExposeExactMimeType(ContentType contentType, String mimeTypeExpected) {
    assertEquals(mimeTypeExpected, contentType.getMimeType());
  }

  @ParameterizedTest
  @EnumSource(ContentType.class)
  void shouldRoundTripMimeTypeForEveryContentType(ContentType contentType) {
    assertEquals(contentType, ContentType.fromMimeType(contentType.getMimeType()));
  }
}
