package net.imaginethinking.appointmentpack.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Carries the email address and password submitted during login.
 */
public record LoginRequest(
        @NotBlank
        @Size(max = 254)
        String email,

        @NotBlank
        String password
) {
}