package io.covena.document.application.port.in;

public record UploadDocumentCommand(String fileName, long fileSize, String declaredMimeType, ContentSource content) {
    @Override
    public String toString() {
        return "UploadDocumentCommand{" +
            "fileName='" + fileName + '\'' +
            ", fileSize=" + fileSize +
            ", declaredMimeType='" + declaredMimeType + '\'' +
            '}';
    }
}
