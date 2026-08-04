package net.imaginethinking.appointmentpack.document.processing;

import net.imaginethinking.appointmentpack.document.DocumentStatus;

import java.util.UUID;

public record DocumentProcessingResultResponse(
        UUID documentId,
        DocumentStatus status,
        String extractedText,
        String generatedSummary,
        String reviewedSummary,
        String processingWarning,
        String processorVersion,
        ModelMetadata model
) {

    public record ModelMetadata(
            String name,
            String revision
    ) {
    }
}
