package net.imaginethinking.appointmentpack.pack;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Carries the appointment and optional patient resources selected for a new Appointment Pack.
 */
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

    /**
     * Copies each selected ID list so the request values cannot be changed after validation.
     */
    public AppointmentPackGenerationRequest {
        medicationIds = immutableList(medicationIds);
        healthcareContactIds = immutableList(healthcareContactIds);
        emergencyContactIds = immutableList(emergencyContactIds);
        medicalHistoryEntryIds = immutableList(medicalHistoryEntryIds);
        bloodTestIds = immutableList(bloodTestIds);
    }

    /**
     * Returns an immutable copy of a selected ID list and uses an empty list when no values were supplied.
     */
    private static List<UUID> immutableList(List<UUID> values) {
        if (values == null) {
            return List.of();
        }

        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}