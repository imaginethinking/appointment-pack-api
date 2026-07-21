package net.imaginethinking.appointmentpack.permission;

import java.util.Set;

public interface Permission {

    String value();

    default Set<String> requiredPermissions() {
        return Set.of();
    }
}
