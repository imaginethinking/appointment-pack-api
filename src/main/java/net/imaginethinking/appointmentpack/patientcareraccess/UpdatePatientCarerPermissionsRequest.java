package net.imaginethinking.appointmentpack.patientcareraccess;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdatePatientCarerPermissionsRequest(
        @NotNull
        Set<@NotBlank String> permissions
) {
}
