package net.imaginethinking.appointmentpack.document.processing.context;

import net.imaginethinking.appointmentpack.document.DocumentType;

import java.util.UUID;

public record DocumentSummarisationContext(
        UUID documentId,
        DocumentType documentType,
        String approvedDeidentifiedText
) {
}