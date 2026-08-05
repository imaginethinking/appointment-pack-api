package net.imaginethinking.appointmentpack.document.processing;

import net.imaginethinking.appointmentpack.document.DocumentStatus;
import net.imaginethinking.appointmentpack.document.DocumentType;

import java.time.Instant;
import java.util.UUID;

public record DocumentProcessingResultResponse(
        UUID documentId,
        DocumentType documentType,
        DocumentStatus status,
        String extractedText,
        String machineDeidentifiedText,
        String approvedDeidentifiedText,
        String generatedSummary,
        String reviewedSummary,
        SummarySource summarySource,
        String processingWarning,
        String processorVersion,
        ModelMetadata model,
        UUID deidentificationReviewedByUserId,
        Instant deidentificationReviewedAt
) {

    public record ModelMetadata(
            String name,
            String promptVersion
    ) {
    }
}