package io.covena.document.application.port.in;

public interface UploadDocumentUseCase {

    UploadedDocumentResult uploadDocument(UploadDocumentCommand command);
}
