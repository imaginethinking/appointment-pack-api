package net.imaginethinking.appointmentpack.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaConfirmRequest(
        @NotBlank
        @Pattern(
                regexp = "\\d{6}",
                message = "MFA code must contain exactly 6 digits"
        )
        String code
) {
}
