package net.imaginethinking.appointmentpack.auth.validation;

import net.imaginethinking.appointmentpack.auth.PasswordChangeRequest;
import net.imaginethinking.appointmentpack.auth.PasswordResetConfirmRequest;
import net.imaginethinking.appointmentpack.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.assertFieldValid;
import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.assertInvalidField;

class StrongPasswordValidationTest {

    @Test
    void shouldAcceptRepresentativeStrongPassword() {
        assertFieldValid(registrationWithPassword("Appointment1!"), "password");
    }

    @Test
    void shouldAcceptStrongPasswordAtMinimumLength() {
        assertFieldValid(registrationWithPassword("Aa1!aaaa"), "password");
    }

    @Test
    void shouldRejectStrongPasswordBelowMinimumLength() {
        assertInvalidField(registrationWithPassword("Aa1!aaa"), "password");
    }

    @Test
    void shouldAcceptStrongPasswordAtMaximumLength() {
        String password = "Aa1!" + "a".repeat(124);

        assertFieldValid(registrationWithPassword(password), "password");
    }

    @Test
    void shouldRejectStrongPasswordAboveMaximumLength() {
        String password = "Aa1!" + "a".repeat(125);

        assertInvalidField(registrationWithPassword(password), "password");
    }

    @ParameterizedTest(name = "should reject password without {0}")
    @MethodSource("passwordsMissingRequiredCharacterClasses")
    void shouldRejectPasswordMissingRequiredCharacterClass(String requirement, String password) {
        assertInvalidField(registrationWithPassword(password), "password");
    }

    @ParameterizedTest(name = "should reject password containing {0}")
    @MethodSource("passwordsContainingLineBreaks")
    void shouldRejectPasswordContainingLineBreak(String description, String password) {
        assertInvalidField(registrationWithPassword(password), "password");
    }

    @Test
    void shouldRejectBlankPassword() {
        assertInvalidField(registrationWithPassword("   "), "password");
    }

    @Test
    void shouldRejectNullPassword() {
        assertInvalidField(registrationWithPassword(null), "password");
    }

    @Test
    void shouldApplySameStrongPasswordPolicyToPasswordReset() {
        PasswordResetConfirmRequest validRequest = new PasswordResetConfirmRequest(
                "reset-token",
                "Appointment1!",
                "Appointment1!"
        );
        PasswordResetConfirmRequest invalidRequest = new PasswordResetConfirmRequest(
                "reset-token",
                "weakpassword",
                "Appointment1!"
        );

        assertFieldValid(validRequest, "newPassword");
        assertInvalidField(invalidRequest, "newPassword");
    }

    @Test
    void shouldApplySameStrongPasswordPolicyToAuthenticatedPasswordChange() {
        PasswordChangeRequest validRequest = new PasswordChangeRequest(
                "Current1!",
                "Appointment1!",
                "Appointment1!"
        );
        PasswordChangeRequest invalidRequest = new PasswordChangeRequest(
                "Current1!",
                "Password12",
                "Appointment1!"
        );

        assertFieldValid(validRequest, "newPassword");
        assertInvalidField(invalidRequest, "newPassword");
    }

    private static Stream<Arguments> passwordsMissingRequiredCharacterClasses() {
        return Stream.of(
                Arguments.of("a lowercase letter", "PASSWORD1!"),
                Arguments.of("an uppercase letter", "password1!"),
                Arguments.of("a number", "Password!!"),
                Arguments.of("a symbol", "Password12"),
                Arguments.of("a non-whitespace symbol", "Password1 ")
        );
    }

    private static Stream<Arguments> passwordsContainingLineBreaks() {
        return Stream.of(
                Arguments.of("a carriage return", "Password1!\r"),
                Arguments.of("a line feed", "Password1!\n"),
                Arguments.of("a CRLF sequence", "Password1!\r\n")
        );
    }

    private static RegisterRequest registrationWithPassword(String password) {
        return new RegisterRequest(
                "patient@example.com",
                password,
                "Appointment1!",
                "Patient",
                "User",
                LocalDate.of(1990, 1, 1)
        );
    }
}