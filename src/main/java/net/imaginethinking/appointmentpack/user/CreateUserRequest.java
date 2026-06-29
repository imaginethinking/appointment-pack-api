package net.imaginethinking.appointmentpack.user;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateUserRequest(
    @Email
    @NotBlank
    String email,

    @NotBlank
    @Size(min = 8)
    String password,

    @NotNull
    UserRole role,

    @NotBlank
    String firstName,

    @NotBlank
    String lastName,

    @NotNull
    @Past
    LocalDate dateOfBirth,

    String gender
) {
}
