package net.imaginethinking.appointmentpack.appointment;

import net.imaginethinking.appointmentpack.patientrecord.PatientRecordPermission;
import net.imaginethinking.appointmentpack.permission.Permission;

import java.util.Set;

/**
 * Defines the patient permissions used for appointment.
 */
public enum AppointmentPermission implements Permission {

    VIEW("appointment:view") {
        /**
         * Returns the permission that must also be granted before this permission can be used.
         */
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(
                    PatientRecordPermission.VIEW.value()
            );
        }
    },

    EDIT("appointment:edit") {
        /**
         * Returns the permission that must also be granted before this permission can be used.
         */
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(
                    VIEW.value()
            );
        }
    };

    private final String value;

    /**
     * Stores the permission string used when this permission is checked.
     */
    AppointmentPermission(String value) {
        this.value = value;
    }

    /**
     * Returns the permission string stored for this permission.
     */
    @Override
    public String value() {
        return value;
    }
}