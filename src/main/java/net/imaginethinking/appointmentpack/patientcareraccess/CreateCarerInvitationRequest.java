package net.imaginethinking.appointmentpack.patientcareraccess;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record CreateCarerInvitationRequest(
        @NotNull (
                message = "Carer email address must be provided"
        )
        @Email
        String carerEmail,

        @NotNull(
                message = "Permissions must not be null"
        )
        Set<@NotBlank String> permissions
) {
}
