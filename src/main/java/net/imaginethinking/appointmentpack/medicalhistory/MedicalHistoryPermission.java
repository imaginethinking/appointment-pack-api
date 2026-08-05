package net.imaginethinking.appointmentpack.medicalhistory;

import net.imaginethinking.appointmentpack.permission.Permission;

import java.util.Set;

public enum MedicalHistoryPermission implements Permission {
    VIEW("history:view"),

    EDIT("history:edit") {
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(VIEW.value());
        }
    };

    private final String value;

    MedicalHistoryPermission(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
