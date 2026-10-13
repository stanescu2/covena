package io.covena.document.application.port.in;

import java.io.InputStream;

public record UploadDocumentCommand(String fileName, long fileSize, String mimeType, InputStream fileStream) {
    @Override
    public String toString() {
        return "UploadDocumentCommand{" +
            "fileName='" + fileName + '\'' +
            ", fileSize=" + fileSize +
            ", mimeType='" + mimeType + '\'' +
            '}';
    }
}
