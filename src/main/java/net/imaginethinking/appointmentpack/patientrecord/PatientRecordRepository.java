package net.imaginethinking.appointmentpack.patientrecord;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Defines the database queries used for patient records.
 */
public interface PatientRecordRepository extends JpaRepository<PatientRecord, UUID> {

    /**
     * Loads the matching patient record when it exists.
     */
    Optional<PatientRecord> findByProfileUserId(UUID userId);
    /**
     * Checks whether a matching patient record already exists.
     */
    boolean existsByProfileId(UUID profileId);

}
