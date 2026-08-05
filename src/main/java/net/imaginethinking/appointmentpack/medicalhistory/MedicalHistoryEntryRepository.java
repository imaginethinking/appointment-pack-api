package net.imaginethinking.appointmentpack.medicalhistory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MedicalHistoryEntryRepository extends JpaRepository<MedicalHistoryEntry, UUID> {
    boolean existsBySourceDocumentId(UUID documentId);

    List<MedicalHistoryEntry> findAllByPatientRecordIdAndArchivedFalseOrderByEntryDateDescCreatedAtDesc(
            UUID patientRecordId
    );
}
