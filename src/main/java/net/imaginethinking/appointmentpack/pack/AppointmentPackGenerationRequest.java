package net.imaginethinking.appointmentpack.pack;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
        List<UUID> medicationIds,

        @Size(max = 100)
        List<UUID> healthcareContactIds,

        @Size(max = 100)
        List<UUID> emergencyContactIds,

        @Size(max = 100)
        List<UUID> medicalHistoryEntryIds,

        @Size(max = 100)
        List<UUID> bloodTestIds
) {

    public AppointmentPackGenerationRequest {
        medicationIds = immutableList(medicationIds);
        healthcareContactIds = immutableList(healthcareContactIds);
        emergencyContactIds = immutableList(emergencyContactIds);
        medicalHistoryEntryIds = immutableList(medicalHistoryEntryIds);
        bloodTestIds = immutableList(bloodTestIds);
    }

    private static List<UUID> immutableList(List<UUID> values) {
        return values == null
                ? List.of()
                : List.copyOf(values);
    }
}