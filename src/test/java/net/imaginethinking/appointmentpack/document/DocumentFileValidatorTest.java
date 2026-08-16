package net.imaginethinking.appointmentpack.document;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentFileValidatorTest {

    private final DocumentFileValidator validator =
            new DocumentFileValidator(10_000);

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
    void shouldRejectDocumentAboveMaximumSize() {
        DocumentFileValidator smallValidator =
                new DocumentFileValidator(4);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "letter.pdf",
                "application/pdf",
                new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> smallValidator.validateAndGetContentType(file)
        );

        assertEquals(
                HttpStatus.PAYLOAD_TOO_LARGE,
                exception.getStatusCode()
        );
    }

    @Test
    void shouldRejectUnsupportedFileSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                "not a supported document".getBytes()
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
}