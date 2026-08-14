package net.imaginethinking.appointmentpack.permission;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PermissionValidator {

    private final PermissionRegistry permissionRegistry;

    public Set<String> validate(Set<String> requestedPermissions) {
        if (requestedPermissions == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Permissions must be provided"
            );
        }

        Set<String> permissions = new HashSet<>(requestedPermissions);
        Set<String> unsupportedPermissions = new HashSet<>(permissions);
        unsupportedPermissions.removeAll(permissionRegistry.supportedValues());

        if (!unsupportedPermissions.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported permissions: " + unsupportedPermissions
            );
        }

        for (String permissionValue : permissions) {
            Permission permission = permissionRegistry
                    .findByValue(permissionValue)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Unsupported permission: " + permissionValue
                    ));

            Set<String> missingPermissions = new HashSet<>(permission.requiredPermissions());

            missingPermissions.removeAll(permissions);

            if (!missingPermissions.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        permissionValue + " requires " + missingPermissions
                );
            }
        }

        return new HashSet<>(permissions);
    }

}
