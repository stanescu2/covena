package io.covena.document.application.port.out;

import io.covena.document.domain.DocumentUploaded;

public interface DocumentEventOutbox {
    void append(DocumentUploaded event);
}
