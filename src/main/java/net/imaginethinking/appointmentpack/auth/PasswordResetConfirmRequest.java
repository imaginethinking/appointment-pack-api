package net.imaginethinking.appointmentpack.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import net.imaginethinking.appointmentpack.auth.validation.StrongPassword;

public record PasswordResetConfirmRequest(
        @NotBlank(message = "Password reset token is required")
        @Size(max = 256)
        String token,

        @StrongPassword
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        @Size(max = 128)
        String confirmPassword
) {
}