package net.imaginethinking.appointmentpack.medication;

import net.imaginethinking.appointmentpack.patientrecord.PatientRecordPermission;
import net.imaginethinking.appointmentpack.permission.Permission;

import java.util.Set;

public enum MedicationPermission implements Permission {

    VIEW("medication:view") {
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(PatientRecordPermission.VIEW.value());
        }
    },

    EDIT("medication:edit") {
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(VIEW.value());
        }
    };

    private final String value;

    MedicationPermission(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}