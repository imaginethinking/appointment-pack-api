package net.imaginethinking.appointmentpack.audit;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.executable.ExecutableValidator;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PatientAuditControllerValidationTest {

    private static final Method GET_AUDIT_EVENTS = getAuditEventsMethod();

    private final ExecutableValidator executableValidator = Validation.buildDefaultValidatorFactory()
            .getValidator()
            .forExecutables();

    private final PatientAuditController controller = new PatientAuditController(
            mock(PatientAuditService.class),
            mock(AuthenticatedUserIdResolver.class));

    @Test
    void shouldAcceptDefaultAuditPaginationValues() {
        assertValidPagination(0, 50);
    }

    @Test
    void shouldAcceptMinimumAuditPageSize() {
        assertValidPagination(0, 1);
    }

    @Test
    void shouldAcceptMaximumAuditPageSize() {
        assertValidPagination(0, 100);
    }

    @Test
    void shouldRejectNegativeAuditPage() {
        assertViolationMessages(validate(-1, 50), Set.of("Page must not be negative"));
    }

    @Test
    void shouldRejectAuditPageSizeBelowMinimum() {
        assertViolationMessages(validate(0, 0), Set.of("Page size must be at least 1"));
    }

    @Test
    void shouldRejectAuditPageSizeAboveMaximum() {
        assertViolationMessages(validate(0, 101), Set.of("Page size must not exceed 100"));
    }

    private void assertValidPagination(int page, int size) {
        Set<ConstraintViolation<PatientAuditController>> violations = validate(page, size);

        assertTrue(
                violations.isEmpty(),
                () -> "Expected audit pagination to be valid but found: " + violationMessages(violations));
    }

    private Set<ConstraintViolation<PatientAuditController>> validate(int page, int size) {
        return executableValidator.validateParameters(
                controller,
                GET_AUDIT_EVENTS,
                new Object[]{null, UUID.randomUUID(), page, size});
    }

    private void assertViolationMessages(
            Set<ConstraintViolation<PatientAuditController>> violations,
            Set<String> expectedMessages) {
        assertEquals(expectedMessages, violationMessages(violations));
    }

    private Set<String> violationMessages(
            Set<ConstraintViolation<PatientAuditController>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    private static Method getAuditEventsMethod() {
        try {
            return PatientAuditController.class.getMethod(
                    "getAuditEvents",
                    Jwt.class,
                    UUID.class,
                    int.class,
                    int.class);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Unable to resolve PatientAuditController.getAuditEvents", exception);
        }
    }
}