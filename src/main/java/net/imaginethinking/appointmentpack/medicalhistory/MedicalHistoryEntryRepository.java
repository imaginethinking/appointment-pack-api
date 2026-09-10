package net.imaginethinking.appointmentpack.medicalhistory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Defines the database queries used for Medical History entries.
 */
public interface MedicalHistoryEntryRepository extends JpaRepository<MedicalHistoryEntry, UUID> {
    /**
     * Checks whether a matching Medical History entry already exists.
     */
    boolean existsBySourceDocumentId(UUID documentId);

    /**
     * Loads the matching Medical History entries with the newest entry first.
     */
    List<MedicalHistoryEntry> findAllByPatientRecord_IdAndArchivedAtIsNullOrderByEntryDateDescCreatedAtDesc(UUID patientRecordId);
}
