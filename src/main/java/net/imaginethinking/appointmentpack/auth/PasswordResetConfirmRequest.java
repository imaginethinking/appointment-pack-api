package net.imaginethinking.appointmentpack.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank(message = "Password reset token is required")
        @Size(max = 256)
        String token,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must contain at least 8 characters")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        @Size(min = 8, message = "Password confirmation must contain at least 8 characters")
        String confirmPassword
) {
}