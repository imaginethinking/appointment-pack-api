package net.imaginethinking.appointmentpack.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import net.imaginethinking.appointmentpack.auth.validation.StrongPassword;

import java.time.LocalDate;

public record RegisterRequest(
        @Email
        @NotBlank
        @Size(max = 254)
        String email,

        @StrongPassword
        String password,

        @NotBlank
        @Size(max = 128)
        String confirmPassword,

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotNull
        @Past
        LocalDate dateOfBirth
) {
}