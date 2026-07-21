package net.imaginethinking.appointmentpack.patientrecord;


import net.imaginethinking.appointmentpack.permission.Permission;

import java.util.Set;

public enum PatientRecordPermission implements Permission {

    VIEW("patient-record:view"),
    EDIT("patient-record:edit") {
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(VIEW.value());
        }
    };

    private final String value;

    PatientRecordPermission(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
