package net.imaginethinking.appointmentpack.auth;

import net.imaginethinking.appointmentpack.auth.mfa.MfaConfirmRequest;
import net.imaginethinking.appointmentpack.auth.mfa.MfaLoginRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.*;

class AuthenticationRequestValidationTest {

    @Test
    void shouldAcceptRepresentativeRegistrationRequest() {
        assertValid(validRegistrationRequest());
    }

    @Test
    void shouldAcceptRegistrationEmailAtAndBelowMaximumLength() {
        assertValid(registrationWithEmail(emailOfLength(253)));
        assertValid(registrationWithEmail(emailOfLength(254)));
    }

    @Test
    void shouldRejectRegistrationEmailAboveMaximumLength() {
        assertInvalidField(registrationWithEmail(emailOfLength(255)), "email");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "not-an-email"})
    void shouldRejectInvalidRegistrationEmail(String email) {
        assertInvalidField(registrationWithEmail(email), "email");
    }

    @ParameterizedTest(name = "{0}: {1} and {2} valid, {3} invalid")
    @MethodSource("registrationNameBoundaries")
    void shouldEnforceRegistrationNameBoundaries(String field, int belowMaximum, int maximum, int aboveMaximum) {
        assertValid(registrationWithName(field, stringOfLength(belowMaximum)));
        assertValid(registrationWithName(field, stringOfLength(maximum)));
        assertInvalidField(registrationWithName(field, stringOfLength(aboveMaximum)), field);
    }

    @ParameterizedTest
    @MethodSource("requiredRegistrationNameValues")
    void shouldRejectBlankOrNullRegistrationNames(String field, String value) {
        assertInvalidField(registrationWithName(field, value), field);
    }

    @Test
    void shouldAcceptPastRegistrationDateOfBirth() {
        assertValid(registrationWithDateOfBirth(LocalDate.now().minusDays(1)));
    }

    @Test
    void shouldRejectPresentRegistrationDateOfBirth() {
        assertInvalidField(registrationWithDateOfBirth(LocalDate.now()), "dateOfBirth");
    }

    @Test
    void shouldRejectFutureRegistrationDateOfBirth() {
        assertInvalidField(registrationWithDateOfBirth(LocalDate.now().plusDays(1)), "dateOfBirth");
    }

    @Test
    void shouldRejectNullRegistrationDateOfBirth() {
        assertInvalidField(registrationWithDateOfBirth(null), "dateOfBirth");
    }

    @Test
    void shouldAcceptRegistrationPasswordConfirmationAtMaximumLength() {
        assertValid(registrationWithConfirmation(stringOfLength(127)));
        assertValid(registrationWithConfirmation(stringOfLength(128)));
    }

    @Test
    void shouldRejectRegistrationPasswordConfirmationAboveMaximumLength() {
        assertInvalidField(registrationWithConfirmation(stringOfLength(129)), "confirmPassword");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectBlankRegistrationPasswordConfirmation(String confirmation) {
        assertInvalidField(registrationWithConfirmation(confirmation), "confirmPassword");
    }

    @Test
    void shouldAcceptRepresentativeLoginRequest() {
        assertValid(new LoginRequest("patient@example.com", "Appointment1!"));
    }

    @Test
    void shouldEnforceLoginEmailLengthBoundary() {
        assertValid(new LoginRequest(emailOfLength(253), "Appointment1!"));
        assertValid(new LoginRequest(emailOfLength(254), "Appointment1!"));
        assertInvalidField(new LoginRequest(emailOfLength(255), "Appointment1!"), "email");
    }

    @Test
    void shouldLeaveLoginEmailSyntaxToAuthenticationLogic() {
        assertValid(new LoginRequest("not-an-email", "Appointment1!"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectBlankLoginEmail(String email) {
        assertInvalidField(new LoginRequest(email, "Appointment1!"), "email");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectBlankLoginPassword(String password) {
        assertInvalidField(new LoginRequest("patient@example.com", password), "password");
    }

    @Test
    void shouldAcceptRecoveryEmailAtMaximumLength() {
        String email = emailOfLength(254);

        assertValid(new EmailVerificationResendRequest(email));
        assertValid(new PasswordResetRequest(email));
    }

    @Test
    void shouldRejectRecoveryEmailAboveMaximumLength() {
        String email = emailOfLength(255);

        assertInvalidField(new EmailVerificationResendRequest(email), "email");
        assertInvalidField(new PasswordResetRequest(email), "email");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "not-an-email"})
    void shouldRejectInvalidRecoveryEmail(String email) {
        assertInvalidField(new EmailVerificationResendRequest(email), "email");
        assertInvalidField(new PasswordResetRequest(email), "email");
    }

    @Test
    void shouldAcceptAccountTokensAtMaximumLength() {
        assertValid(new EmailVerificationConfirmRequest(stringOfLength(255)));
        assertValid(new EmailVerificationConfirmRequest(stringOfLength(256)));

        assertValid(passwordResetWithToken(stringOfLength(255)));
        assertValid(passwordResetWithToken(stringOfLength(256)));
    }

    @Test
    void shouldRejectAccountTokensAboveMaximumLength() {
        assertInvalidField(new EmailVerificationConfirmRequest(stringOfLength(257)), "token");

        assertInvalidField(passwordResetWithToken(stringOfLength(257)), "token");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectBlankAccountTokens(String token) {
        assertInvalidField(new EmailVerificationConfirmRequest(token), "token");
        assertInvalidField(passwordResetWithToken(token), "token");
    }

    @Test
    void shouldEnforcePasswordResetConfirmationLengthBoundary() {
        assertValid(passwordResetWithConfirmation(stringOfLength(127)));
        assertValid(passwordResetWithConfirmation(stringOfLength(128)));

        assertInvalidField(passwordResetWithConfirmation(stringOfLength(129)), "confirmPassword");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectBlankPasswordResetConfirmation(String confirmation) {
        assertInvalidField(passwordResetWithConfirmation(confirmation), "confirmPassword");
    }

    @Test
    void shouldEnforceCurrentPasswordLengthBoundary() {
        assertValid(passwordChangeWithCurrentPassword(stringOfLength(127)));
        assertValid(passwordChangeWithCurrentPassword(stringOfLength(128)));

        assertInvalidField(passwordChangeWithCurrentPassword(stringOfLength(129)), "currentPassword");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectBlankCurrentPassword(String currentPassword) {
        assertInvalidField(passwordChangeWithCurrentPassword(currentPassword), "currentPassword");
    }

    @Test
    void shouldEnforcePasswordChangeConfirmationLengthBoundary() {
        assertValid(passwordChangeWithConfirmation(stringOfLength(127)));
        assertValid(passwordChangeWithConfirmation(stringOfLength(128)));

        assertInvalidField(passwordChangeWithConfirmation(stringOfLength(129)), "confirmPassword");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectBlankPasswordChangeConfirmation(String confirmation) {
        assertInvalidField(passwordChangeWithConfirmation(confirmation), "confirmPassword");
    }

    @Test
    void shouldAcceptSixDigitMfaCodes() {
        UUID challengeId = UUID.randomUUID();

        assertValid(new MfaConfirmRequest("123456"));
        assertValid(new MfaLoginRequest(challengeId, "123456"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"12345", "1234567", "abcdef", "12a456", "12 456", "   "}
    )
    void shouldRejectInvalidMfaCodeFormats(String code) {
        UUID challengeId = UUID.randomUUID();

        assertInvalidField(new MfaConfirmRequest(code), "code");
        assertInvalidField(new MfaLoginRequest(challengeId, code), "code");
    }

    @Test
    void shouldRejectNullMfaCodes() {
        UUID challengeId = UUID.randomUUID();

        assertInvalidField(new MfaConfirmRequest(null), "code");
        assertInvalidField(new MfaLoginRequest(challengeId, null), "code");
    }

    @Test
    void shouldRejectNullMfaChallengeId() {
        assertInvalidField(new MfaLoginRequest(null, "123456"), "mfaChallengeId");
    }

    private static Stream<Arguments> registrationNameBoundaries() {
        return Stream.of(Arguments.of("firstName", 99, 100, 101), Arguments.of("lastName", 99, 100, 101));
    }

    private static Stream<Arguments> requiredRegistrationNameValues() {
        return Stream.of(
                Arguments.of("firstName", ""),
                Arguments.of("firstName", "   "),
                Arguments.of("firstName", null),
                Arguments.of("lastName", ""),
                Arguments.of("lastName", "   "),
                Arguments.of("lastName", null));
    }

    private static RegisterRequest validRegistrationRequest() {
        return new RegisterRequest(
                "patient@example.com",
                "Appointment1!",
                "Appointment1!",
                "Patient",
                "User",
                LocalDate.of(1990, 1, 1));
    }

    private static RegisterRequest registrationWithEmail(String email) {
        return new RegisterRequest(
                email,
                "Appointment1!",
                "Appointment1!",
                "Patient",
                "User",
                LocalDate.of(1990, 1, 1));
    }

    private static RegisterRequest registrationWithName(String field, String value) {
        return switch (field) {
            case "firstName" -> new RegisterRequest(
                    "patient@example.com",
                    "Appointment1!",
                    "Appointment1!",
                    value,
                    "User",
                    LocalDate.of(1990, 1, 1));
            case "lastName" -> new RegisterRequest(
                    "patient@example.com",
                    "Appointment1!",
                    "Appointment1!",
                    "Patient",
                    value,
                    LocalDate.of(1990, 1, 1));
            default -> throw new IllegalArgumentException("Unsupported registration name field: " + field);
        };
    }

    private static RegisterRequest registrationWithDateOfBirth(LocalDate dateOfBirth) {
        return new RegisterRequest(
                "patient@example.com",
                "Appointment1!",
                "Appointment1!",
                "Patient",
                "User",
                dateOfBirth);
    }

    private static RegisterRequest registrationWithConfirmation(String confirmation) {
        return new RegisterRequest(
                "patient@example.com",
                "Appointment1!",
                confirmation,
                "Patient",
                "User",
                LocalDate.of(1990, 1, 1));
    }

    private static PasswordResetConfirmRequest passwordResetWithToken(String token) {
        return new PasswordResetConfirmRequest(token, "Appointment1!", "Appointment1!");
    }

    private static PasswordResetConfirmRequest passwordResetWithConfirmation(
            String confirmation) {
        return new PasswordResetConfirmRequest("reset-token", "Appointment1!", confirmation);
    }

    private static PasswordChangeRequest passwordChangeWithCurrentPassword(
            String currentPassword) {
        return new PasswordChangeRequest(currentPassword, "Appointment1!", "Appointment1!");
    }

    private static PasswordChangeRequest passwordChangeWithConfirmation(
            String confirmation) {
        return new PasswordChangeRequest("Current1!", "Appointment1!", confirmation);
    }

    private static String emailOfLength(int length) {
        String prefix = "a@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(63) + ".";

        int remainingCharacters = length - prefix.length();

        if (remainingCharacters < 1 || remainingCharacters > 63) {
            throw new IllegalArgumentException("Unsupported synthetic email length: " + length);
        }

        return prefix + "e".repeat(remainingCharacters);
    }
}