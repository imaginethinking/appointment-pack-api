package net.imaginethinking.appointmentpack.document.processing.context;

import net.imaginethinking.appointmentpack.document.DocumentType;

import java.util.UUID;

/**
 * Keeps the values needed while document extraction is being processed.
 */
public record DocumentExtractionContext(
        UUID documentId,
        DocumentType documentType,
        String originalFileName,
        String contentType,
        String storagePath,
        RedactionContext redactionContext
) {
}
