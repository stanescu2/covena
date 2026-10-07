package io.covena.document.domain;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileNameTest {

  @Test
  void shouldCreateFileNameWhenValid() {
    String name = "validName";
    FileName fileName = new FileName(name);
    assertEquals(name, fileName.name());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "  ", "\t", "\n", "\r"})
  void shouldRejectNullOrBlankName(String name) {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new FileName(name));
    assertEquals("Name cannot be null or empty", ex.getMessage());
  }

  @Test
  void shouldRejectNameLongerThanMaxLength() {
    String longName = "a".repeat(256);
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new FileName(longName));
    assertEquals("Name cannot be longer than " + FileName.MAX_LENGTH + " characters", ex.getMessage());
  }

  @Test
  void shouldAcceptNameWithMaxLength() {
    String maxLenName = "a".repeat(255);
    FileName fileName = new FileName(maxLenName);
    assertEquals(maxLenName, fileName.name());
  }

  @Test
  void shouldRejectNameContainingSlash() {
    String slashName = "a/b/c";
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new FileName(slashName));
    assertEquals("Name cannot contain '/' or '\\' characters", ex.getMessage());
  }

  @Test
  void shouldRejectNameContainingBackslash() {
    String backSlashName = "a\\b\\c";
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new FileName(backSlashName));
    assertEquals("Name cannot contain '/' or '\\' characters", ex.getMessage());
  }

  @Test
  void shouldPreserveExactNameValue() {
    String original = "  My File (final).pdf  ";
    FileName fileName = new FileName(original);
    assertEquals(original, fileName.name());
  }

  @Test
  void shouldSupportEqualityForValueObject() {
    FileName fileName1 = new FileName("myTestFileName");
    FileName fileName2 = new FileName("myTestFileName");
    assertEquals(fileName1, fileName2);
  }
}
