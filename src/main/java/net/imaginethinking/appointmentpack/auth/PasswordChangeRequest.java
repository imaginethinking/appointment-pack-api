package net.imaginethinking.appointmentpack.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import net.imaginethinking.appointmentpack.auth.validation.StrongPassword;

public record PasswordChangeRequest(
        @NotBlank(message = "Current password is required")
        @Size(max = 128)
        String currentPassword,

        @StrongPassword
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        @Size(max = 128)
        String confirmPassword
) {
}