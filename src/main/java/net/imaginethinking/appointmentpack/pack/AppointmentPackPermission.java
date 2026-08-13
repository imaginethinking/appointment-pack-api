package net.imaginethinking.appointmentpack.pack;

import net.imaginethinking.appointmentpack.patientrecord.PatientRecordPermission;
import net.imaginethinking.appointmentpack.permission.Permission;

import java.util.Set;

public enum AppointmentPackPermission implements Permission {

    VIEW("appointment-pack:view") {
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(PatientRecordPermission.VIEW.value());
        }
    },

    CREATE("appointment-pack:create") {
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(VIEW.value());
        }
    };

    private final String value;

    AppointmentPackPermission(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}