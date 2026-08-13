package net.imaginethinking.appointmentpack.document.processing.api;

import jakarta.validation.constraints.NotBlank;

public record DocumentSummarisationRequest(
        @NotBlank(message = "Approved de-identified text is required")
        String approvedDeidentifiedText
) {
}
