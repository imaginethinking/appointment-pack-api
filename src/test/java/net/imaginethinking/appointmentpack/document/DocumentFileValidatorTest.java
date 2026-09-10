package net.imaginethinking.appointmentpack.document;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Checks the validation rules used for document file.
 */
class DocumentFileValidatorTest {

    private static final long MAXIMUM_FILE_SIZE = 8;

    private final DocumentFileValidator validator =
            new DocumentFileValidator(MAXIMUM_FILE_SIZE);

    @Test
    void shouldAcceptPdfSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "letter.pdf",
                "application/octet-stream",
                new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37}
        );

        assertEquals(
                "application/pdf",
                validator.validateAndGetContentType(file)
        );
    }

    @Test
    void shouldAcceptPngSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scan.png",
                "application/octet-stream",
                new byte[]{
                        (byte) 0x89,
                        0x50,
                        0x4E,
                        0x47,
                        0x0D,
                        0x0A,
                        0x1A,
                        0x0A
                }
        );

        assertEquals(
                "image/png",
                validator.validateAndGetContentType(file)
        );
    }

    @Test
    void shouldAcceptJpegSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scan.jpg",
                "application/octet-stream",
                new byte[]{
                        (byte) 0xFF,
                        (byte) 0xD8,
                        (byte) 0xFF,
                        0x00
                }
        );

        assertEquals(
                "image/jpeg",
                validator.validateAndGetContentType(file)
        );
    }

    @Test
    void shouldAcceptDocumentImmediatelyBelowMaximumSize() {
        MockMultipartFile file = pdfFileOfSize(7);

        assertEquals(
                "application/pdf",
                validator.validateAndGetContentType(file)
        );
    }

    @Test
    void shouldAcceptDocumentAtMaximumSize() {
        MockMultipartFile file = pdfFileOfSize(8);

        assertEquals(
                "application/pdf",
                validator.validateAndGetContentType(file)
        );
    }

    @Test
    void shouldRejectDocumentImmediatelyAboveMaximumSize() {
        MockMultipartFile file = pdfFileOfSize(9);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> validator.validateAndGetContentType(file)
        );

        assertEquals(
                HttpStatus.PAYLOAD_TOO_LARGE,
                exception.getStatusCode()
        );
    }

    @Test
    void shouldRejectNullDocument() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> validator.validateAndGetContentType(null)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }

    @Test
    void shouldRejectEmptyDocument() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> validator.validateAndGetContentType(file)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }

    @Test
    void shouldRejectUnsupportedFileSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                new byte[]{0x01, 0x02, 0x03, 0x04, 0x05}
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> validator.validateAndGetContentType(file)
        );

        assertEquals(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                exception.getStatusCode()
        );
    }

    @Test
    void shouldReturnUnprocessableContentWhenFileCannotBeRead() throws IOException {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(5L);
        when(file.getInputStream()).thenThrow(new IOException("read failure"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> validator.validateAndGetContentType(file)
        );

        assertEquals(
                HttpStatus.UNPROCESSABLE_CONTENT,
                exception.getStatusCode()
        );
    }

    /**
     * Creates a PDF upload with the requested byte size.
     */
    private MockMultipartFile pdfFileOfSize(int size) {
        if (size < 5) {
            throw new IllegalArgumentException(
                    "PDF test data must include the full signature"
            );
        }

        byte[] content = new byte[size];
        content[0] = 0x25;
        content[1] = 0x50;
        content[2] = 0x44;
        content[3] = 0x46;
        content[4] = 0x2D;

        return new MockMultipartFile(
                "file",
                "letter.pdf",
                "application/octet-stream",
                content
        );
    }
}