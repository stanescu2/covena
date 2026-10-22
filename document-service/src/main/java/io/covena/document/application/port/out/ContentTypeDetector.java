package io.covena.document.application.port.out;

import io.covena.document.application.port.in.ContentSource;

public interface ContentTypeDetector {
    String detectContentType(ContentSource content, String fileName);
}
