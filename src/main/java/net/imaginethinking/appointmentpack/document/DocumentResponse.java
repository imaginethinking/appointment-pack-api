package net.imaginethinking.appointmentpack.document;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents document information returned by the API.
 */
public record DocumentResponse(
        UUID id,
        UUID patientRecordId,
        DocumentType documentType,
        DocumentStatus status,
        String originalFileName,
        String contentType,
        long fileSize,
        Instant createdAt
) {
}
