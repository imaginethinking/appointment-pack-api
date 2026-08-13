package net.imaginethinking.appointmentpack.pack;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record AppointmentPackGenerationRequest(

        @NotNull(message = "Appointment is required")
        UUID appointmentId,

        @Size(
                max = 250,
                message = "Pack title must not exceed 250 characters"
        )
        String title,

        @Size(
                max = 2000,
                message = "Pack notes must not exceed 2000 characters"
        )
        String notes,

        @Size(max = 100)
        List<@NotNull UUID> medicationIds,

        @Size(max = 100)
        List<@NotNull UUID> healthcareContactIds,

        @Size(max = 100)
        List<@NotNull UUID> emergencyContactIds,

        @Size(max = 100)
        List<@NotNull UUID> medicalHistoryEntryIds,

        @Size(max = 100)
        List<@NotNull UUID> bloodTestIds
) {

    public AppointmentPackGenerationRequest {
        medicationIds = immutableList(medicationIds);
        healthcareContactIds = immutableList(healthcareContactIds);
        emergencyContactIds = immutableList(emergencyContactIds);
        medicalHistoryEntryIds = immutableList(medicalHistoryEntryIds);
        bloodTestIds = immutableList(bloodTestIds);
    }

    private static List<UUID> immutableList(List<UUID> values) {
        if (values == null) {
            return List.of();
        }

        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}