package net.imaginethinking.appointmentpack.bloodtest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateBloodTestRequest(

        @Size(
                max = 200,
                message = "Blood test title must not exceed 200 characters"
        ) String title,

        @NotNull(message = "Blood test date is required") LocalDate testDate,

        @Size(
                max = 200,
                message = "Provider must not exceed 200 characters"
        ) String provider,

        @Size(
                max = 2000,
                message = "Notes must not exceed 2000 characters"
        ) String notes,

        @Valid @NotEmpty(message = "At least one blood test result is required") @Size(
                max = 100,
                message = "A blood test cannot contain more than 100 results"
        ) List<BloodTestResultRequest> results) {
}