package net.imaginethinking.appointmentpack.bloodtest;

import net.imaginethinking.appointmentpack.patientrecord.PatientRecordPermission;
import net.imaginethinking.appointmentpack.permission.Permission;

import java.util.Set;

public enum BloodTestPermission implements Permission {

    VIEW("blood-result:view") {
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(PatientRecordPermission.VIEW.value());
        }
    },

    EDIT("blood-result:edit") {
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(VIEW.value());
        }
    };

    private final String value;

    BloodTestPermission(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}