package net.imaginethinking.appointmentpack.patientcareraccess;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Carries the changes submitted when updating patient carer permissions.
 */
public record UpdatePatientCarerPermissionsRequest(
        @NotNull
        Set<@NotBlank String> permissions
) {
}
