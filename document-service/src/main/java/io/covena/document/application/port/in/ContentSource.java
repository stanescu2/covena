package io.covena.document.application.port.in;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface ContentSource {
    InputStream open() throws IOException;
}
