package net.imaginethinking.appointmentpack.patientcareraccess;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record CreateCarerInvitationRequest(
        @NotNull (
                message = "Carer user ID must be provided"
        )
        UUID carerUserId,

        @NotNull(
                message = "Permissions must not be null"
        )
        Set<@NotBlank String> permissions
) {
}
