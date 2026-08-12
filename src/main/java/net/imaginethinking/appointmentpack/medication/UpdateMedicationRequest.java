package net.imaginethinking.appointmentpack.medication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateMedicationRequest(
        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 100)
        String dose,

        @Size(max = 100)
        String form,

        @Size(max = 500)
        String instructions,

        LocalDate startDate,

        LocalDate endDate,

        @Size(max = 2000)
        String notes
) {
}