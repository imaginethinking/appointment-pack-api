package net.imaginethinking.appointmentpack.medicalhistory;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.common.TextNormalizer;
import net.imaginethinking.appointmentpack.document.Document;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import net.imaginethinking.appointmentpack.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicalHistoryService {

    private final MedicalHistoryEntryRepository medicalHistoryEntryRepository;
    private final PatientRecordAccessService patientRecordAccessService;
    private final EntityManager entityManager;

    @Transactional
    public MedicalHistoryEntryResponse createMedicalHistoryEntry(
            UUID authenticatedUserId,
            UUID patientRecordId,
            CreateMedicalHistoryEntryRequest request) {
        PatientRecord patientRecord = patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecordId,
                MedicalHistoryPermission.EDIT);

        MedicalHistoryEntry entry = new MedicalHistoryEntry();

        entry.setPatientRecord(patientRecord);
        entry.setTitle(TextNormalizer.strip(request.title()));
        entry.setSummary(TextNormalizer.strip(request.summary()));
        entry.setEntryDate(request.entryDate());
        entry.setSourceType(MedicalHistorySourceType.MANUAL);
        entry.setSourceDocument(null);
        entry.setCreatedBy(entityManager.getReference(User.class, authenticatedUserId));

        MedicalHistoryEntry savedEntry = medicalHistoryEntryRepository.save(entry);

        return toResponse(savedEntry);
    }

    @Transactional(readOnly = true)
    public List<MedicalHistoryEntryResponse> getMedicalHistoryEntries(UUID authenticatedUserId, UUID patientRecordId) {
        patientRecordAccessService.requireAccess(authenticatedUserId, patientRecordId, MedicalHistoryPermission.VIEW);

        return medicalHistoryEntryRepository.findAllByPatientRecord_IdAndArchivedAtIsNullOrderByEntryDateDescCreatedAtDesc(
                patientRecordId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MedicalHistoryEntryResponse getMedicalHistoryEntry(UUID authenticatedUserId, UUID entryId) {
        MedicalHistoryEntry entry = findAvailableEntry(entryId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                entry.getPatientRecord(),
                MedicalHistoryPermission.VIEW);

        return toResponse(entry);
    }

    @Transactional
    public MedicalHistoryEntryResponse updateMedicalHistoryEntry(
            UUID authenticatedUserId,
            UUID entryId,
            UpdateMedicalHistoryEntryRequest request) {
        MedicalHistoryEntry entry = findAvailableEntry(entryId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                entry.getPatientRecord(),
                MedicalHistoryPermission.EDIT);

        entry.setTitle(TextNormalizer.strip(request.title()));
        entry.setSummary(TextNormalizer.strip(request.summary()));
        entry.setEntryDate(request.entryDate());

        return toResponse(entry);
    }

    @Transactional
    public MedicalHistoryEntryResponse archiveMedicalHistoryEntry(UUID authenticatedUserId, UUID entryId) {
        MedicalHistoryEntry entry = findEntry(entryId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                entry.getPatientRecord(),
                MedicalHistoryPermission.EDIT);

        entry.archive();

        return toResponse(entry);
    }

    private MedicalHistoryEntry findEntry(UUID entryId) {
        return medicalHistoryEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Medical history entry not found"));
    }

    private MedicalHistoryEntry findAvailableEntry(UUID entryId) {
        MedicalHistoryEntry entry = findEntry(entryId);

        if (entry.isArchived()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical history entry not found");
        }

        return entry;
    }

    private MedicalHistoryEntryResponse toResponse(MedicalHistoryEntry entry) {
        Document sourceDocument = entry.getSourceDocument();

        return new MedicalHistoryEntryResponse(
                entry.getId(),
                entry.getPatientRecord().getId(),
                entry.getTitle(),
                entry.getSummary(),
                entry.getEntryDate(),
                entry.getSourceType(),
                sourceDocument == null ? null : sourceDocument.getId(),
                sourceDocument == null ? null : sourceDocument.getDocumentType(),
                entry.getCreatedBy().getId(),
                entry.getArchivedAt());
    }
}