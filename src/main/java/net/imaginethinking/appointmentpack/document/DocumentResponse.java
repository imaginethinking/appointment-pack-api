package net.imaginethinking.appointmentpack.document;

import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID patientRecordId,
        DocumentType documentType,
        DocumentStatus status,
        String originalFileName,
        String contentType,
        long fileSize
) {
}
