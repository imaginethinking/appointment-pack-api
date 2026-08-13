package net.imaginethinking.appointmentpack.auth.mfa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

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
