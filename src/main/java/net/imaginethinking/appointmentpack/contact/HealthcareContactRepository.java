package net.imaginethinking.appointmentpack.contact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Defines the database queries used for healthcare contacts.
 */
public interface HealthcareContactRepository extends JpaRepository<HealthcareContact, UUID> {

    /**
     * Loads the matching healthcare contacts in name order.
     */
    List<HealthcareContact> findAllByPatientRecord_IdAndArchivedAtIsNullOrderByNameAsc(UUID patientRecordId);
}