package net.imaginethinking.appointmentpack.auth.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import net.imaginethinking.appointmentpack.auth.PasswordResetConfirmRequest;
import net.imaginethinking.appointmentpack.auth.RegisterRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrongPasswordValidationTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void shouldAcceptStrongPasswordForRegistration() {
        RegisterRequest request = new RegisterRequest(
                "patient@example.com",
                "Appointment1!",
                "Appointment1!",
                "Patient",
                "User",
                LocalDate.of(1990, 1, 1)
        );

        boolean passwordViolation = validator.validate(request)
                .stream()
                .anyMatch(violation ->
                        violation.getPropertyPath().toString().equals("password"));

        assertFalse(passwordViolation);
    }

    @Test
    void shouldRejectCommonWeakPasswordPatterns() {
        List<String> weakPasswords = List.of(
                "password1!",
                "PASSWORD1!",
                "Password!!",
                "Password12",
                "Pass1!"
        );

        for (String password : weakPasswords) {
            RegisterRequest request = new RegisterRequest(
                    "patient@example.com",
                    password,
                    password,
                    "Patient",
                    "User",
                    LocalDate.of(1990, 1, 1)
            );

            boolean passwordViolation = validator.validate(request)
                    .stream()
                    .anyMatch(violation ->
                            violation.getPropertyPath().toString().equals("password"));

            assertTrue(
                    passwordViolation,
                    "Expected password to be rejected: " + password
            );
        }
    }

    @Test
    void shouldApplySamePolicyToPasswordReset() {
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(
                "reset-token",
                "weakpassword",
                "weakpassword"
        );

        boolean passwordViolation = validator.validate(request)
                .stream()
                .anyMatch(violation ->
                        violation.getPropertyPath().toString().equals("newPassword"));

        assertTrue(passwordViolation);
    }
}