package net.imaginethinking.appointmentpack.contact;

import net.imaginethinking.appointmentpack.patientrecord.PatientRecordPermission;
import net.imaginethinking.appointmentpack.permission.Permission;

import java.util.Set;

public enum ContactPermission implements Permission {

    VIEW("contact:view") {
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(PatientRecordPermission.VIEW.value());
        }
    },

    EDIT("contact:edit") {
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(VIEW.value());
        }
    };

    private final String value;

    ContactPermission(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}