package net.imaginethinking.appointmentpack.document.processing.api;

import net.imaginethinking.appointmentpack.document.DocumentStatus;
import net.imaginethinking.appointmentpack.document.DocumentType;
import net.imaginethinking.appointmentpack.document.processing.SummarySource;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents document processing result information returned by the API.
 */
public record DocumentProcessingResultResponse(
        UUID documentId,
        DocumentType documentType,
        DocumentStatus status,
        String extractedText,
        String machineDeidentifiedText,
        String approvedDeidentifiedText,
        AppointmentDetailsResponse appointmentDetails,
        String generatedSummary,
        String reviewedSummary,
        SummarySource summarySource,
        String processingWarning,
        String processorVersion,
        ModelMetadata model,
        UUID appointmentReviewedByUserId,
        Instant appointmentReviewedAt,
        UUID deidentificationReviewedByUserId,
        Instant deidentificationReviewedAt,
        UUID summaryReviewedByUserId,
        Instant summaryReviewedAt
) {

    /**
     * Keeps the processor, model and prompt details returned with a generated summary.
     */
    public record ModelMetadata(
            String name,
            String promptVersion
    ) {
    }
}