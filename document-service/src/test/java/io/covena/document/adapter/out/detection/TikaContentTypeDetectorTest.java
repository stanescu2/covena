package io.covena.document.adapter.out.detection;

import io.covena.document.application.port.in.ContentSource;
import io.covena.document.application.port.out.ContentTypeDetectionException;
import io.covena.document.application.port.out.ContentTypeDetector;
import io.covena.document.domain.ContentType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TikaContentTypeDetectorTest {
    private static final byte[] PDF_CONTENT = "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] TEXT_CONTENT = "Acesta nu este un PDF".getBytes(StandardCharsets.UTF_8);
    private static final byte[] UNKNOWN_BINARY_CONTENT = {0x01, 0x02, 0x03, 0x04};

    private static final String PDF_FILE_NAME = "contract.pdf";
    private static final String JPG_FILE_NAME = "poza.jpg";
    private static final String DOCX_FILE_NAME = "minimal.docx";

    private static final String PDF_MIME_TYPE = ContentType.PDF.getMimeType();
    private static final String DOCX_MIME_TYPE = ContentType.DOCX.getMimeType();
    private static final String TEXT_MIME_TYPE = "text/plain";

    private final ContentTypeDetector contentTypeDetector = new TikaContentTypeDetector();

    private static byte[] minimalDocx() throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("<Types/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    @Test
    void shouldDetectPdfFromContent() {
        assertThat(contentTypeDetector
            .detectContentType(() -> new ByteArrayInputStream(PDF_CONTENT), PDF_FILE_NAME))
            .isEqualTo(PDF_MIME_TYPE);
    }

    @Test
    void shouldDetectPdfEvenWhenFileNameSaysOtherwise() {
        assertThat(contentTypeDetector
            .detectContentType(() -> new ByteArrayInputStream(PDF_CONTENT), JPG_FILE_NAME))
            .isEqualTo(PDF_MIME_TYPE);
    }

    @Test
    void shouldDetectPlainTextDisguisedAsPdf() {
        assertThat(contentTypeDetector
            .detectContentType(() -> new ByteArrayInputStream(TEXT_CONTENT), PDF_FILE_NAME))
            .isEqualTo(TEXT_MIME_TYPE);
    }

    @Test
    void shouldDetectDocxFromZipStructureAndFileName() {
        assertThat(contentTypeDetector
            .detectContentType(() -> new ByteArrayInputStream(minimalDocx()), DOCX_FILE_NAME))
            .isEqualTo(DOCX_MIME_TYPE);
    }

    @Test
    void shouldTrustFileNameWhenContentHasNoKnownSignature() {
        assertThat(contentTypeDetector
            .detectContentType(() -> new ByteArrayInputStream(UNKNOWN_BINARY_CONTENT), PDF_FILE_NAME))
            .isEqualTo(PDF_MIME_TYPE);
    }

    @Test
    void shouldWrapIOExceptionInContentTypeDetectionException() {
        ContentSource failingSource = () -> {throw new IOException("boom");};
        assertThatThrownBy(() -> contentTypeDetector.detectContentType(failingSource, PDF_FILE_NAME))
            .isExactlyInstanceOf(ContentTypeDetectionException.class)
            .hasMessage("Failed to detect content type for file " + PDF_FILE_NAME)
            .hasCauseInstanceOf(IOException.class);
    }
}
