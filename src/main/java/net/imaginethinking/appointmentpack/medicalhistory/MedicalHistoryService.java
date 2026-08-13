package net.imaginethinking.appointmentpack.medicalhistory;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.document.Document;
import net.imaginethinking.appointmentpack.patientcareraccess.PatientAccessControlService;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordRepository;
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

    private final PatientRecordRepository patientRecordRepository;
    private final MedicalHistoryEntryRepository medicalHistoryEntryRepository;
    private final PatientAccessControlService patientAccessControlService;
    private final EntityManager entityManager;

    @Transactional
    public MedicalHistoryEntryResponse createEntry(
            UUID authenticatedUserId,
            UUID patientRecordId,
            MedicalHistoryEntryRequest request) {
        PatientRecord patientRecord = findPatientRecord(patientRecordId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                patientRecord,
                MedicalHistoryPermission.EDIT);

        MedicalHistoryEntry entry = new MedicalHistoryEntry();

        entry.setPatientRecord(patientRecord);
        entry.setTitle(request.title().strip());
        entry.setSummary(request.summary().strip());
        entry.setEntryDate(request.entryDate());
        entry.setSourceType(MedicalHistorySourceType.MANUAL);
        entry.setSourceDocument(null);
        entry.setCreatedBy(entityManager.getReference(User.class, authenticatedUserId));

        MedicalHistoryEntry savedEntry = medicalHistoryEntryRepository.save(entry);

        return toResponse(savedEntry);
    }

    @Transactional(readOnly = true)
    public List<MedicalHistoryEntryResponse> getHistory(UUID authenticatedUserId, UUID patientRecordId) {
        PatientRecord patientRecord = findPatientRecord(patientRecordId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                patientRecord,
                MedicalHistoryPermission.VIEW);

        return medicalHistoryEntryRepository.findAllByPatientRecord_IdAndArchivedAtIsNullOrderByEntryDateDescCreatedAtDesc(
                patientRecordId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MedicalHistoryEntryResponse getEntry(UUID authenticatedUserId, UUID entryId) {
        MedicalHistoryEntry entry = findAvailableEntry(entryId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                entry.getPatientRecord(),
                MedicalHistoryPermission.VIEW);

        return toResponse(entry);
    }

    @Transactional
    public MedicalHistoryEntryResponse updateEntry(
            UUID authenticatedUserId,
            UUID entryId,
            MedicalHistoryEntryRequest request) {
        MedicalHistoryEntry entry = findAvailableEntry(entryId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                entry.getPatientRecord(),
                MedicalHistoryPermission.EDIT);

        entry.setTitle(request.title().strip());
        entry.setSummary(request.summary().strip());
        entry.setEntryDate(request.entryDate());

        return toResponse(entry);
    }

    @Transactional
    public MedicalHistoryEntryResponse archiveEntry(UUID authenticatedUserId, UUID entryId) {
        MedicalHistoryEntry entry = findEntry(entryId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                entry.getPatientRecord(),
                MedicalHistoryPermission.EDIT);

        entry.archive();

        return toResponse(entry);
    }

    private PatientRecord findPatientRecord(UUID patientRecordId) {
        return patientRecordRepository.findById(patientRecordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient record not found"));
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