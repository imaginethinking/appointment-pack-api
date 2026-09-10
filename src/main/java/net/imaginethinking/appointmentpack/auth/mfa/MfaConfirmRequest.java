package net.imaginethinking.appointmentpack.auth.mfa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Carries the authenticator code used to confirm or disable MFA.
 */
public record MfaConfirmRequest(
        @NotBlank
        @Pattern(
                regexp = "\\d{6}",
                message = "MFA code must contain exactly 6 digits"
        )
        String code
) {
}
