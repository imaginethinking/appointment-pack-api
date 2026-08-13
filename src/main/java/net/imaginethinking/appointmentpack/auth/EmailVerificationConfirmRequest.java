package net.imaginethinking.appointmentpack.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailVerificationConfirmRequest(
        @NotBlank(message = "Verification token is required")
        @Size(max = 256)
        String token
) {
}