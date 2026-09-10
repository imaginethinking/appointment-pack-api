package net.imaginethinking.appointmentpack.medicalhistory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Carries the values submitted when creating Medical History entry.
 */
public record CreateMedicalHistoryEntryRequest(

        @NotBlank(message = "History title is required") @Size(
                max = 200,
                message = "History title must not exceed 200 characters"
        )
        String title,

        @NotBlank(message = "History summary is required") @Size(
                max = 10000,
                message = "History summary must not exceed 10000 characters"
        )
        String summary,

        @NotNull(message = "History date is required") @PastOrPresent(
                message = "History date must not be in the future"
        )
        LocalDate entryDate
) {
}