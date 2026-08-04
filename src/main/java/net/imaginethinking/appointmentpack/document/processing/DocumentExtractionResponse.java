package net.imaginethinking.appointmentpack.document.processing;

import java.util.UUID;

public record DocumentExtractionResponse(
        UUID documentId,
        String extractedText,
        String deidentifiedText,
        String generatedSummary,
        String processingWarning,
        String processorVersion
) {
}