package net.imaginethinking.appointmentpack.document;

import net.imaginethinking.appointmentpack.permission.Permission;

import java.util.Set;

public enum DocumentPermission implements Permission {

    VIEW("document:view"),
    EDIT("document:edit") {
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(VIEW.value());
        }
    },
    UPLOAD("document:upload") {
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(EDIT.value());
        }
    };

    private final String value;

    DocumentPermission(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

}
