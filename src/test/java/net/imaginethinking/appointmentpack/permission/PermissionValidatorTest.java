package net.imaginethinking.appointmentpack.permission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermissionValidatorTest {

    private PermissionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PermissionValidator(new PermissionRegistry());
    }

    @Test
    void shouldAcceptSupportedPermissionsWithRequiredDependencies() {
        Set<String> permissions = validator.validate(Set.of("patient-record:view", "document:view", "document:edit"));

        assertEquals(3, permissions.size());

        assertTrue(permissions.contains("document:edit"));
    }

    @Test
    void shouldRejectUnsupportedPermission() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> validator.validate(Set.of("unknown:permission")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldRejectPermissionWithoutRequiredDependency() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> validator.validate(Set.of("document:view")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}