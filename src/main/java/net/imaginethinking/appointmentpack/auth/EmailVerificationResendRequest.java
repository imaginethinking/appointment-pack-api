package net.imaginethinking.appointmentpack.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Carries the email address used when requesting another verification message.
 */
public record EmailVerificationResendRequest(
        @NotBlank(message = "Email address is required")
        @Email(message = "Email address must be valid")
        @Size(max = 254)
        String email
) {
}