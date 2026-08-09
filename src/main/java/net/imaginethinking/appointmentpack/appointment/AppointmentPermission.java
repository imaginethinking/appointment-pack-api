package net.imaginethinking.appointmentpack.appointment;

import net.imaginethinking.appointmentpack.patientrecord.PatientRecordPermission;
import net.imaginethinking.appointmentpack.permission.Permission;

import java.util.Set;

public enum AppointmentPermission implements Permission {

    VIEW("appointment:view") {
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(
                    PatientRecordPermission.VIEW.value()
            );
        }
    },

    EDIT("appointment:edit") {
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(
                    VIEW.value()
            );
        }
    };

    private final String value;

    AppointmentPermission(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}