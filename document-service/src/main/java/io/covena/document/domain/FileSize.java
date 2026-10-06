package io.covena.document.domain;

public record FileSize(long fileSizeBytes) {
  public static final long MAX_SIZE_BYTES = 50L * 1024 * 1024;
  public FileSize {
    if (fileSizeBytes <= 0) {
      throw new IllegalArgumentException("File size cannot be negative or zero");
    }
    if (fileSizeBytes > MAX_SIZE_BYTES) {
      throw new IllegalArgumentException("File size cannot exceed " + MAX_SIZE_BYTES + " bytes");
    }
  }
}
