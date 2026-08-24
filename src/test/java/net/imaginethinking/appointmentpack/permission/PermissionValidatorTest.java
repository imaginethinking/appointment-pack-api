package net.imaginethinking.appointmentpack.permission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermissionValidatorTest {

    private PermissionRegistry permissionRegistry;
    private PermissionValidator validator;

    @BeforeEach
    void setUp() {
        permissionRegistry = new PermissionRegistry();
        validator = new PermissionValidator(permissionRegistry);
    }

    @Test
    void shouldAcceptSupportedPermissionsWithRequiredDependencies() {
        Set<String> permissions = validator.validate(Set.of("patient-record:view", "document:view", "document:edit"));

        assertEquals(3, permissions.size());
        assertTrue(permissions.contains("document:edit"));
    }

    @Test
    void shouldAcceptEmptyPermissionSet() {
        Set<String> permissions = validator.validate(Set.of());

        assertTrue(permissions.isEmpty());
    }

    @Test
    void shouldAcceptCompleteSupportedPermissionCatalogue() {
        Set<String> supportedPermissions = permissionRegistry.supportedValues();

        Set<String> permissions = validator.validate(supportedPermissions);

        assertEquals(supportedPermissions, permissions);
    }

    @Test
    void shouldRejectNullPermissionSet() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> validator.validate(null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
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

    @Test
    void shouldRejectPermissionWhenTransitiveDependencyIsMissing() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> validator.validate(Set.of("document:view", "document:edit")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldAcceptPermissionWhenFullDependencyChainIsPresent() {
        Set<String> permissions = validator.validate(Set.of(
                "patient-record:view",
                "document:view",
                "document:edit",
                "document:upload"));

        assertEquals(4, permissions.size());
        assertTrue(permissions.contains("document:upload"));
    }
}