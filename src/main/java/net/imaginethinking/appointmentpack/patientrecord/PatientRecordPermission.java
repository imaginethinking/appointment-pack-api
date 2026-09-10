package net.imaginethinking.appointmentpack.patientrecord;


import net.imaginethinking.appointmentpack.permission.Permission;

import java.util.Set;

/**
 * Defines the patient permissions used for patient record.
 */
public enum PatientRecordPermission implements Permission {

    VIEW("patient-record:view"),
    EDIT("patient-record:edit") {
        /**
         * Returns the permission that must also be granted before this permission can be used.
         */
        @Override
        public Set<String> requiredPermissions() {
            return Set.of(VIEW.value());
        }
    };

    private final String value;

    /**
     * Stores the permission string used when this permission is checked.
     */
    PatientRecordPermission(String value) {
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
