package net.imaginethinking.appointmentpack.permission;

import net.imaginethinking.appointmentpack.appointment.AppointmentPermission;
import net.imaginethinking.appointmentpack.audit.AuditPermission;
import net.imaginethinking.appointmentpack.bloodtest.BloodTestPermission;
import net.imaginethinking.appointmentpack.contact.ContactPermission;
import net.imaginethinking.appointmentpack.document.DocumentPermission;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryPermission;
import net.imaginethinking.appointmentpack.medication.MedicationPermission;
import net.imaginethinking.appointmentpack.pack.AppointmentPackPermission;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordPermission;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Provides the supported patient permissions and their required permissions in one place.
 */
@Component
public class PermissionRegistry {

    private final Map<String, Permission> permissions;

    /**
     * Registers each permission type and keeps an immutable lookup by permission value.
     */
    public PermissionRegistry() {
        Map<String, Permission> registeredPermissions = new HashMap<>();

        register(registeredPermissions, PatientRecordPermission.values());
        register(registeredPermissions, DocumentPermission.values());
        register(registeredPermissions, MedicalHistoryPermission.values());
        register(registeredPermissions, AppointmentPermission.values());
        register(registeredPermissions, MedicationPermission.values());
        register(registeredPermissions, ContactPermission.values());
        register(registeredPermissions, BloodTestPermission.values());
        register(registeredPermissions, AppointmentPackPermission.values());
        register(registeredPermissions, AuditPermission.values());

        permissions= Map.copyOf(registeredPermissions);
    }

    /**
     * Looks up a supported permission using its stored string value.
     */
    public Optional<Permission> findByValue(String value) {
        return Optional.ofNullable(permissions.get(value));
    }

    /**
     * Returns all permission strings supported by the application.
     */
    public Set<String> supportedValues() {
        return permissions.keySet();
    }

    /**
     * Adds one permission type to the lookup and fails fast if two permissions use the same value.
     */
    private void register(
            Map<String, Permission> registeredPermissions,
            Permission[] permissionsToRegister
    ) {
        for (Permission permission : permissionsToRegister) {
            Permission existing = registeredPermissions.putIfAbsent(permission.value(), permission);

            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate permission value: " + permission.value()
                );
            }
        }
    }
}
