package net.imaginethinking.appointmentpack.document.processing.context;

import net.imaginethinking.appointmentpack.document.DocumentType;

import java.util.UUID;

/**
 * Keeps the values needed while document summarisation is being processed.
 */
public record DocumentSummarisationContext(
        UUID documentId,
        DocumentType documentType,
        String approvedDeidentifiedText
) {
}