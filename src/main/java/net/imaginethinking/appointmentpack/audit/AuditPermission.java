package net.imaginethinking.appointmentpack.audit;

import net.imaginethinking.appointmentpack.patientrecord.PatientRecordPermission;
import net.imaginethinking.appointmentpack.permission.Permission;

import java.util.Set;

public enum AuditPermission implements Permission {

    VIEW("audit:view") {
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(PatientRecordPermission.VIEW.value());
        }
    };

    private final String value;

    AuditPermission(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}