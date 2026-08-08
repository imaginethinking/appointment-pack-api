package net.imaginethinking.appointmentpack.document;

import java.time.Instant;
import java.util.UUID;

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
