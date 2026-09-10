package net.imaginethinking.appointmentpack.testsupport;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the validation rules used for validation test support.
 */
public final class ValidationTestSupport {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * Creates the test fixture with the supplied values.
     */
    private ValidationTestSupport() {
    }

    /**
     * Checks that the supplied value passes validation.
     */
    public static <T> void assertValid(T value) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(value);

        assertTrue(
                violations.isEmpty(),
                () -> "Expected request to be valid but found violations: " + describe(violations));
    }

    /**
     * Checks that the supplied value fails validation.
     */
    public static <T> void assertInvalid(T value) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(value);

        assertFalse(violations.isEmpty(), "Expected request to be invalid but no validation violations were found");
    }

    /**
     * Checks that validation fails for the expected field.
     */
    public static <T> void assertInvalidField(T value, String field) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(value);

        assertTrue(
                hasViolationForField(violations, field),
                () -> "Expected validation violation for field '" + field + "' but found: " + describe(violations));
    }

    /**
     * Checks that the expected field has no validation error.
     */
    public static <T> void assertFieldValid(T value, String field) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(value);

        assertFalse(
                hasViolationForField(violations, field),
                () -> "Expected field '" + field + "' to be valid but found: " + describe(violations));
    }

    /**
     * Creates a string with the requested length for boundary validation tests.
     */
    public static String stringOfLength(int length) {
        return "a".repeat(length);
    }

    /**
     * Checks whether the validation result contains an error for the requested field.
     */
    private static boolean hasViolationForField(Set<? extends ConstraintViolation<?>> violations, String field) {
        return violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals(field));
    }

    /**
     * Formats validation errors so failed test assertions are easier to read.
     */
    private static String describe(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));
    }
}