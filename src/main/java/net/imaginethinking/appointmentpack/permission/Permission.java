package net.imaginethinking.appointmentpack.permission;

import java.util.Set;

/**
 * Defines the value and required permissions shared by patient scoped permission types.
 */
public interface Permission {

    /**
     * Returns the permission string stored for this permission.
     */
    String value();

    /**
     * Allows this permission without requiring another patient permission first.
     */
    default Set<String> requiredPermissions() {
        return Set.of();
    }
}
