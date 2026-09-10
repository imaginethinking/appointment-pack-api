package net.imaginethinking.appointmentpack.contact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Defines the database queries used for emergency contacts.
 */
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, UUID> {

    /**
     * Loads the matching emergency contacts in name order.
     */
    List<EmergencyContact> findAllByPatientRecord_IdAndArchivedAtIsNullOrderByNameAsc(UUID patientRecordId);
}