package net.imaginethinking.appointmentpack.auth.mfa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * Carries the MFA challenge ID and authenticator code used to finish login.
 */
public record MfaLoginRequest(

        @NotNull
        UUID mfaChallengeId,

        @NotBlank
        @Pattern(
                regexp = "\\d{6}",
                message = "MFA code must contain exactly 6 digits"
        )
        String code
) {
}
