package io.covena.document.domain;

public enum ContentType {
  PDF("application/pdf"),
  DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
  private final String mimeType;

  ContentType(String mimeType) {
    this.mimeType = mimeType;
  }

  public static ContentType fromMimeType(String mimeType) {
    for (ContentType contentType : values()) {
      if (contentType.mimeType.equalsIgnoreCase(mimeType)) {
        return contentType;
      }
    }
    throw new IllegalArgumentException("Unknown mime type: " + mimeType);
  }

  public String getMimeType() {
    return mimeType;
  }
}
