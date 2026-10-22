package io.covena.document.adapter.out.detection;

import io.covena.document.application.port.in.ContentSource;
import io.covena.document.application.port.out.ContentTypeDetectionException;
import io.covena.document.application.port.out.ContentTypeDetector;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
class TikaContentTypeDetector implements ContentTypeDetector {
    private static final Logger logger = LoggerFactory.getLogger(TikaContentTypeDetector.class);
    private static final String FAILURE_TO_DETECT_MESSAGE_TEMPLATE = "Failed to detect content type for file %s";
    private static final String DETECT_SUCCESS_LOG_MESSAGE = "Successfully detected content type {} for file {}";
    private final Tika tika = new Tika();

    @Override
    public String detectContentType(ContentSource content, String fileName) {
        try (InputStream in = content.open()) {
            String contentType = tika.detect(in, fileName);
            logger.debug(DETECT_SUCCESS_LOG_MESSAGE, contentType, fileName);
            return contentType;
        } catch (IOException e) {
            throw new ContentTypeDetectionException(FAILURE_TO_DETECT_MESSAGE_TEMPLATE.formatted(fileName), e);
        }
    }
}
