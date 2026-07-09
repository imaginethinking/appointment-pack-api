package net.imaginethinking.appointmentpack.auth;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegisterRequest(
        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 8)
        String password,

        @NotBlank
        @Size(min = 8)
        String confirmPassword,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotNull
        @Past
        LocalDate dateOfBirth
) {
}
