package net.imaginethinking.appointmentpack.document.processing;

import java.util.UUID;

public record DocumentProcessingResponse(
        UUID documentId,
        String extractedText,
        String summary,
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