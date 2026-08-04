package net.imaginethinking.appointmentpack.document.processing;

import net.imaginethinking.appointmentpack.document.DocumentType;

import java.util.UUID;

public record DocumentExtractionContext(
        UUID documentId,
        DocumentType documentType,
        String originalFileName,
        String contentType,
        String storagePath,
        RedactionContext redactionContext
) {
}
