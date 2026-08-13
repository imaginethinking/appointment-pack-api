package net.imaginethinking.appointmentpack.permission;

import net.imaginethinking.appointmentpack.appointment.AppointmentPermission;
import net.imaginethinking.appointmentpack.bloodtest.BloodTestPermission;
import net.imaginethinking.appointmentpack.contact.ContactPermission;
import net.imaginethinking.appointmentpack.document.DocumentPermission;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryPermission;
import net.imaginethinking.appointmentpack.medication.MedicationPermission;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordPermission;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class PermissionRegistry {

    private final Map<String, Permission> permissions;

    public PermissionRegistry() {
        Map<String, Permission> registeredPermissions = new HashMap<>();

        register(registeredPermissions, PatientRecordPermission.values());
        register(registeredPermissions, DocumentPermission.values());
        register(registeredPermissions, MedicalHistoryPermission.values());
        register(registeredPermissions, AppointmentPermission.values());
        register(registeredPermissions, MedicationPermission.values());
        register(registeredPermissions, ContactPermission.values());
        register(registeredPermissions, BloodTestPermission.values());

        permissions= Map.copyOf(registeredPermissions);
    }

    public Optional<Permission> findByValue(String value) {
        return Optional.ofNullable(permissions.get(value));
    }

    public Set<String> supportedValues() {
        return permissions.keySet();
    }

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
