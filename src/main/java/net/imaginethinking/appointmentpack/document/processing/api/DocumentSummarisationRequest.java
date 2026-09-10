package net.imaginethinking.appointmentpack.document.processing.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Carries the exact deidentified consultation text approved for summarisation.
 */
public record DocumentSummarisationRequest(
        @NotBlank(message = "Approved de-identified text is required")
        @Size(
                max = 100000,
                message = "Approved de-identified text must not exceed 100000 characters"
        )
        String approvedDeidentifiedText
) {
}