package net.imaginethinking.appointmentpack.document.processing.client;

import net.imaginethinking.appointmentpack.document.processing.api.AppointmentDetailsResponse;

import java.util.UUID;

/**
 * Represents document extraction information returned by the API.
 */
public record DocumentExtractionResponse(
        UUID documentId,
        String extractedText,
        String deidentifiedText,
        AppointmentDetailsResponse appointmentDetails,
        String processingWarning,
        String processorVersion
) {
}