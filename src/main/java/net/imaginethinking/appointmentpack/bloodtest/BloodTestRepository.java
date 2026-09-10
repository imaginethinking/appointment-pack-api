package net.imaginethinking.appointmentpack.bloodtest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Defines the database queries used for blood tests.
 */
public interface BloodTestRepository
        extends JpaRepository<BloodTest, UUID> {

    /**
     * Loads the matching blood tests with the newest test first.
     */
    List<BloodTest> findAllByPatientRecord_IdAndArchivedAtIsNullOrderByTestDateDescCreatedAtDesc(UUID patientRecordId);
}