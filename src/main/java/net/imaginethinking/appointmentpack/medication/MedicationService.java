package net.imaginethinking.appointmentpack.medication;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.patientcareraccess.PatientAccessControlService;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final PatientRecordRepository patientRecordRepository;
    private final PatientAccessControlService patientAccessControlService;

    @Transactional
    public MedicationResponse createMedication(
            UUID authenticatedUserId,
            UUID patientRecordId,
            CreateMedicationRequest request
    ) {
        PatientRecord patientRecord = findPatientRecord(patientRecordId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                patientRecord,
                MedicationPermission.EDIT
        );

        validateDates(request.startDate(), request.endDate());

        Medication medication = new Medication();

        medication.setPatientRecord(patientRecord);
        applyValues(
                medication,
                request.name(),
                request.dose(),
                request.form(),
                request.instructions(),
                request.startDate(),
                request.endDate(),
                request.notes()
        );

        Medication savedMedication = medicationRepository.save(medication);

        return MedicationResponse.from(savedMedication);
    }

    @Transactional(readOnly = true)
    public List<MedicationResponse> getMedications(
            UUID authenticatedUserId,
            UUID patientRecordId
    ) {
        PatientRecord patientRecord = findPatientRecord(patientRecordId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                patientRecord,
                MedicationPermission.VIEW
        );

        return medicationRepository
                .findAllByPatientRecord_IdAndArchivedFalseOrderByStartDateDescCreatedAtDesc(patientRecordId)
                .stream()
                .map(MedicationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MedicationResponse getMedication(
            UUID authenticatedUserId,
            UUID medicationId
    ) {
        Medication medication = findAvailableMedication(medicationId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                medication.getPatientRecord(),
                MedicationPermission.VIEW
        );

        return MedicationResponse.from(medication);
    }

    @Transactional
    public MedicationResponse updateMedication(
            UUID authenticatedUserId,
            UUID medicationId,
            UpdateMedicationRequest request
    ) {
        Medication medication = findAvailableMedication(medicationId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                medication.getPatientRecord(),
                MedicationPermission.EDIT
        );

        validateDates(request.startDate(), request.endDate());

        applyValues(
                medication,
                request.name(),
                request.dose(),
                request.form(),
                request.instructions(),
                request.startDate(),
                request.endDate(),
                request.notes()
        );

        return MedicationResponse.from(medication);
    }

    @Transactional
    public MedicationResponse archiveMedication(
            UUID authenticatedUserId,
            UUID medicationId
    ) {
        Medication medication = findMedication(medicationId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                medication.getPatientRecord(),
                MedicationPermission.EDIT
        );

        if (!medication.isArchived()) {
            medication.setArchived(true);
        }

        return MedicationResponse.from(medication);
    }

    private PatientRecord findPatientRecord(UUID patientRecordId) {
        return patientRecordRepository
                .findById(patientRecordId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient record not found"
                ));
    }

    private Medication findMedication(UUID medicationId) {
        return medicationRepository
                .findById(medicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Medication not found"
                ));
    }

    private Medication findAvailableMedication(UUID medicationId) {
        Medication medication = findMedication(medicationId);

        if (medication.isArchived()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Medication not found"
            );
        }

        return medication;
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null
                && endDate != null
                && endDate.isBefore(startDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Medication end date cannot be before start date"
            );
        }
    }

    private void applyValues(
            Medication medication,
            String name,
            String dose,
            String form,
            String instructions,
            LocalDate startDate,
            LocalDate endDate,
            String notes
    ) {
        medication.setName(name.strip());
        medication.setDose(normaliseOptionalValue(dose));
        medication.setForm(normaliseOptionalValue(form));
        medication.setInstructions(normaliseOptionalValue(instructions));
        medication.setStartDate(startDate);
        medication.setEndDate(endDate);
        medication.setNotes(normaliseOptionalValue(notes));
    }

    private String normaliseOptionalValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.strip();
    }
}