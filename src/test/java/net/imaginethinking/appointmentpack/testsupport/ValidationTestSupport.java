package net.imaginethinking.appointmentpack.testsupport;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ValidationTestSupport {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private ValidationTestSupport() {
    }

    public static <T> void assertValid(T value) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(value);

        assertTrue(
                violations.isEmpty(),
                () -> "Expected request to be valid but found violations: " + describe(violations));
    }

    public static <T> void assertInvalid(T value) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(value);

        assertFalse(violations.isEmpty(), "Expected request to be invalid but no validation violations were found");
    }

    public static <T> void assertInvalidField(T value, String field) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(value);

        assertTrue(
                hasViolationForField(violations, field),
                () -> "Expected validation violation for field '" + field + "' but found: " + describe(violations));
    }

    public static <T> void assertFieldValid(T value, String field) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(value);

        assertFalse(
                hasViolationForField(violations, field),
                () -> "Expected field '" + field + "' to be valid but found: " + describe(violations));
    }

    public static String stringOfLength(int length) {
        return "a".repeat(length);
    }

    private static boolean hasViolationForField(Set<? extends ConstraintViolation<?>> violations, String field) {
        return violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals(field));
    }

    private static String describe(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));
    }
}