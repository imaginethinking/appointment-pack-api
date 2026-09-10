package net.imaginethinking.appointmentpack.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Checks document size and file signatures before an upload is accepted.
 */
@Component
public class DocumentFileValidator {

    // [AI-ASSISTED: ChatGPT, 2026-08-08]
    // AI was used to help generate the file signatures for PDF, PNG and JPEG files.
    private static final byte[] PDF_SIGNATURE = {0x25, 0x50, 0x44, 0x46, 0x2D};

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private final long maximumFileSize;

    /**
     * Creates the validator using the maximum document size configured for uploads.
     */
    public DocumentFileValidator(
            @Value("${appointment-pack.documents.maximum-file-size-bytes}") long maximumFileSize) {
        this.maximumFileSize = maximumFileSize;
    }

    /**
     * Checks that the upload is present, within the size limit and has a supported PDF or image file signature.
     */
    public String validateAndGetContentType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document file must not be empty");
        }

        if (file.getSize() > maximumFileSize) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Document file exceeds the maximum size");
        }

        try (InputStream inputStream = file.getInputStream()) {
            byte[] signature = inputStream.readNBytes(8);

            if (startsWith(signature, PDF_SIGNATURE)) {
                return "application/pdf";
            }

            if (startsWith(signature, PNG_SIGNATURE)) {
                return "image/png";
            }

            if (startsWith(signature, JPEG_SIGNATURE)) {
                return "image/jpeg";
            }

            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only PDF, JPEG and PNG documents are supported");
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "Document file could not be read",
                    exception);
        }
    }

    /**
     * Checks whether the uploaded bytes begin with the expected file signature.
     */
    private boolean startsWith(byte[] value, byte[] prefix) {
        return value.length >= prefix.length && Arrays.equals(value, 0, prefix.length, prefix, 0, prefix.length);
    }
}