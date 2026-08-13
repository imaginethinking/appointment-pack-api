package net.imaginethinking.appointmentpack.document.processing.api;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record DocumentSummaryAcceptanceRequest(
        @NotBlank(message = "Reviewed summary is required")
        String reviewedSummary,

        @NotBlank(message = "History title is required")
        @Size(max = 200, message = "History title must not exceed 200 characters")
        String historyTitle,

        @NotNull(message = "History date is required")
        @PastOrPresent(message = "History date must not be in the future")
        LocalDate historyDate) {
}