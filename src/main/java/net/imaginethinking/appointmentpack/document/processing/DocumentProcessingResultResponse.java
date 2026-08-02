package net.imaginethinking.appointmentpack.document.processing;

import net.imaginethinking.appointmentpack.document.DocumentStatus;

import java.util.List;
import java.util.UUID;

public record DocumentProcessingResultResponse(
        UUID documentId,
        DocumentStatus status,
        String extractedText,
        String generatedSummary,
        String reviewedSummary,
        List<DocumentProcessingKeyPoint> keyPoints,
        List<String> warnings,
        String processorVersion,
        ModelMetadata model
) {

    public record ModelMetadata(
            String name,
            String revision
    ) {
    }
}
