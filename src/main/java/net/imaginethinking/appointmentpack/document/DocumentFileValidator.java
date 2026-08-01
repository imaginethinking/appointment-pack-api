package net.imaginethinking.appointmentpack.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@Component
public class DocumentFileValidator {
    private static final byte[] PDF_SIGNATURE = {
            0x25, 0x50, 0x44, 0x46, 0x2D
    };

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
    };

    private static final byte[] JPEG_SIGNATURE = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
    };

    private final long maximumFileSize;

    public DocumentFileValidator(
            @Value("${appointment-pack.documents.maximum-file-size-bytes}")
            long maximumFileSize
    ) {
        this.maximumFileSize = maximumFileSize;
    }

    public String validateAndGetContentType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Document file must not be empty");
        }

        if (file.getSize() > maximumFileSize) {
            throw new IllegalArgumentException("Document file exceeds the maximum size");
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

            throw new IllegalArgumentException(
                    "Only PDF, JPEG and PNG documents are supported"
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to inspect document file",
                    exception
            );
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        return value.length >= prefix.length
                && Arrays.equals(
                value,
                0,
                prefix.length,
                prefix,
                0,
                prefix.length
        );
    }
}
