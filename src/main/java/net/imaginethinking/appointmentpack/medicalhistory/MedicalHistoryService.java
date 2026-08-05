package net.imaginethinking.appointmentpack.medicalhistory;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.document.Document;
import net.imaginethinking.appointmentpack.patientcareraccess.PatientAccessControlService;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordRepository;
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

    @Transactional(readOnly = true)
    public List<MedicalHistoryEntryResponse> getHistory(UUID authenticatedUserId, UUID patientRecordId) {
        PatientRecord patientRecord = patientRecordRepository.findById(patientRecordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient record not found"));

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                patientRecord,
                MedicalHistoryPermission.VIEW);

        return medicalHistoryEntryRepository.findAllByPatientRecordIdAndArchivedFalseOrderByEntryDateDescCreatedAtDesc(
                patientRecordId).stream().map(this::toResponse).toList();
    }

    private MedicalHistoryEntryResponse toResponse(MedicalHistoryEntry entry) {
        Document sourceDocument = entry.getSourceDocument();

        UUID sourceDocumentId = sourceDocument == null ? null : sourceDocument.getId();

        return new MedicalHistoryEntryResponse(
                entry.getId(),
                entry.getTitle(),
                entry.getSummary(),
                entry.getEntryDate(),
                entry.getSourceType(),
                sourceDocumentId,
                sourceDocument == null ? null : sourceDocument.getDocumentType());
    }
}