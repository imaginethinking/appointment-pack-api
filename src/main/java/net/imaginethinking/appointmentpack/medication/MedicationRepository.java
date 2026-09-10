package net.imaginethinking.appointmentpack.medication;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Defines the database queries used for medications.
 */
public interface MedicationRepository extends JpaRepository<Medication, UUID> {

    /**
     * Loads the matching medications with the most recent start date first.
     */
    List<Medication> findAllByPatientRecord_IdAndArchivedAtIsNullOrderByStartDateDescCreatedAtDesc(UUID patientRecordId);
}