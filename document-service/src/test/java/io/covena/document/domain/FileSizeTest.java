package io.covena.document.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class FileSizeTest {

  @Test
  void shouldCreateFileSizeWhenValid() {
    long fileSizeBytes = 1;
    FileSize fileSize = new FileSize(fileSizeBytes);
    assertEquals(fileSizeBytes, fileSize.fileSizeBytes());
  }

  @ParameterizedTest
  @ValueSource(longs = {Long.MIN_VALUE, -1, 0})
  void shouldRejectFileSizeWhenNegativeOrZero(long invalidFileSize) {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new FileSize(invalidFileSize));
    assertEquals("File size cannot be negative or zero", ex.getMessage());
  }

  @Test
  void shouldRejectFileSizeWhenGreaterThanMaxSizeBytes() {
    long invalidFileSize = FileSize.MAX_SIZE_BYTES + 1;
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new FileSize(invalidFileSize));
    assertEquals("File size cannot exceed " + FileSize.MAX_SIZE_BYTES + " bytes", ex.getMessage());
  }

  @Test
  void shouldCreateFileSizeWhenValidMaxSizeBytes() {
    long fileSizeBytes = FileSize.MAX_SIZE_BYTES;
    FileSize fileSize = new FileSize(fileSizeBytes);
    assertEquals(fileSizeBytes, fileSize.fileSizeBytes());
  }

  @Test
  void shouldSupportEqualityForValueObject() {
    FileSize fileSize1 = new FileSize(1);
    FileSize fileSize2 = new FileSize(1);
    assertEquals(fileSize1, fileSize2);
  }
}