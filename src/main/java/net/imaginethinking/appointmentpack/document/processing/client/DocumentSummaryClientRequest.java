package net.imaginethinking.appointmentpack.document.processing.client;

import java.util.UUID;

/**
 * Carries the document ID and approved deidentified text sent for summarisation.
 */
public record DocumentSummaryClientRequest(
        UUID documentId,
        String approvedDeidentifiedText
) {
}
