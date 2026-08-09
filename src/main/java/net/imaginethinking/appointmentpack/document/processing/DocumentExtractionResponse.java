package net.imaginethinking.appointmentpack.document.processing;

import java.util.UUID;

public record DocumentExtractionResponse(
        UUID documentId,
        String extractedText,
        String deidentifiedText,
        AppointmentDetailsResponse appointmentDetails,
        String processingWarning,
        String processorVersion
) {
}