package net.imaginethinking.appointmentpack.patientcareraccess;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateCarerInvitationRequest(
        @NotBlank(message = "Carer email address must be provided")
        @Email
        @Size(max = 254)
        String carerEmail,

        @NotNull(message = "Permissions must not be null")
        Set<@NotBlank String> permissions
) {
}