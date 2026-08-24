package net.imaginethinking.appointmentpack.analytics.admin;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.executable.ExecutableValidator;
import net.imaginethinking.appointmentpack.analytics.OperationalEventCategory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AdminAnalyticsControllerValidationTest {

    private static final Method GET_EVENTS = getEventsMethod();

    private final ExecutableValidator executableValidator = Validation.buildDefaultValidatorFactory()
            .getValidator()
            .forExecutables();

    private final AdminAnalyticsController controller = new AdminAnalyticsController(mock(AdminAnalyticsService.class));

    @Test
    void shouldAcceptDefaultAnalyticsPaginationValues() {
        assertValidPagination(0, 100);
    }

    @Test
    void shouldAcceptMinimumAnalyticsPageSize() {
        assertValidPagination(0, 1);
    }

    @Test
    void shouldAcceptMaximumAnalyticsPageSize() {
        assertValidPagination(0, 100);
    }

    @Test
    void shouldRejectNegativeAnalyticsPage() {
        assertViolationMessages(validate(-1, 100), Set.of("Page must not be negative"));
    }

    @Test
    void shouldRejectAnalyticsPageSizeBelowMinimum() {
        assertViolationMessages(validate(0, 0), Set.of("Page size must be at least 1"));
    }

    @Test
    void shouldRejectAnalyticsPageSizeAboveMaximum() {
        assertViolationMessages(validate(0, 101), Set.of("Page size must not exceed 100"));
    }

    private void assertValidPagination(int page, int size) {
        Set<ConstraintViolation<AdminAnalyticsController>> violations = validate(page, size);

        assertTrue(
                violations.isEmpty(),
                () -> "Expected analytics pagination to be valid but found: " + violationMessages(violations));
    }

    private Set<ConstraintViolation<AdminAnalyticsController>> validate(int page, int size) {
        return executableValidator.validateParameters(
                controller,
                GET_EVENTS,
                new Object[]{null, null, null, page, size});
    }

    private void assertViolationMessages(
            Set<ConstraintViolation<AdminAnalyticsController>> violations,
            Set<String> expectedMessages) {
        assertEquals(expectedMessages, violationMessages(violations));
    }

    private Set<String> violationMessages(
            Set<ConstraintViolation<AdminAnalyticsController>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    private static Method getEventsMethod() {
        try {
            return AdminAnalyticsController.class.getMethod(
                    "getEvents",
                    Instant.class,
                    Instant.class,
                    OperationalEventCategory.class,
                    int.class,
                    int.class);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Unable to resolve AdminAnalyticsController.getEvents", exception);
        }
    }
}