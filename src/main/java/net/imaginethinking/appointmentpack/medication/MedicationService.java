package net.imaginethinking.appointmentpack.medication;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.common.TextNormalizer;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Creates and updates medications while checking patient access and medication date ranges.
 */
@Service
@RequiredArgsConstructor
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final PatientRecordAccessService patientRecordAccessService;
    private final AppEventPublisher appEventPublisher;

    /**
     * Checks edit access and validates the submitted values before saving the new medication.
     */
    @Transactional
    public MedicationResponse createMedication(
            UUID authenticatedUserId,
            UUID patientRecordId,
            CreateMedicationRequest request) {
        PatientRecord patientRecord = patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecordId,
                MedicationPermission.EDIT);

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
                request.notes());

        Medication savedMedication = medicationRepository.save(medication);

        publishActivity(
                authenticatedUserId,
                savedMedication,
                PatientActivityAction.CREATED);

        return MedicationResponse.from(savedMedication);
    }

    /**
     * Checks view access before returning the active medications with the most recent start date first.
     */
    @Transactional(readOnly = true)
    public List<MedicationResponse> getMedications(UUID authenticatedUserId, UUID patientRecordId) {
        patientRecordAccessService.requireAccess(authenticatedUserId, patientRecordId, MedicationPermission.VIEW);

        return medicationRepository.findAllByPatientRecord_IdAndArchivedAtIsNullOrderByStartDateDescCreatedAtDesc(
                patientRecordId).stream().map(MedicationResponse::from).toList();
    }

    /**
     * Loads the requested medication and checks that the current user can view its patient record.
     */
    @Transactional(readOnly = true)
    public MedicationResponse getMedication(UUID authenticatedUserId, UUID medicationId) {
        Medication medication = findAvailableMedication(medicationId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                medication.getPatientRecord(),
                MedicationPermission.VIEW);

        return MedicationResponse.from(medication);
    }

    /**
     * Loads the current medication, checks edit access and applies the submitted changes.
     */
    @Transactional
    public MedicationResponse updateMedication(
            UUID authenticatedUserId,
            UUID medicationId,
            UpdateMedicationRequest request) {
        Medication medication = findAvailableMedication(medicationId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                medication.getPatientRecord(),
                MedicationPermission.EDIT);

        validateDates(request.startDate(), request.endDate());

        applyValues(
                medication,
                request.name(),
                request.dose(),
                request.form(),
                request.instructions(),
                request.startDate(),
                request.endDate(),
                request.notes());

        publishActivity(
                authenticatedUserId,
                medication,
                PatientActivityAction.UPDATED);

        return MedicationResponse.from(medication);
    }

    /**
     * Checks access and archives the medication only when it is still active.
     */
    @Transactional
    public MedicationResponse archiveMedication(UUID authenticatedUserId, UUID medicationId) {
        Medication medication = findMedication(medicationId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                medication.getPatientRecord(),
                MedicationPermission.EDIT);

        if (!medication.isArchived()) {
            medication.archive();

            publishActivity(
                    authenticatedUserId,
                    medication,
                    PatientActivityAction.ARCHIVED);
        }

        return MedicationResponse.from(medication);
    }

    /**
     * Loads the medication or returns not found when it does not exist.
     */
    private Medication findMedication(UUID medicationId) {
        return medicationRepository.findById(medicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medication not found"));
    }

    /**
     * Loads the medication and treats an archived record as not found.
     */
    private Medication findAvailableMedication(UUID medicationId) {
        Medication medication = findMedication(medicationId);

        if (medication.isArchived()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Medication not found");
        }

        return medication;
    }

    /**
     * Publishes the activity event for the completed change.
     */
    private void publishActivity(
            UUID authenticatedUserId,
            Medication medication,
            PatientActivityAction action) {
        appEventPublisher.publish(PatientActivityEvent.create(
                authenticatedUserId,
                medication.getPatientRecord().getId(),
                PatientResourceType.MEDICATION,
                medication.getId(),
                action));
    }

    /**
     * Checks that the medication end date is not earlier than its start date.
     */
    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Medication end date cannot be before start date");
        }
    }

    /**
     * Copies the submitted medication details onto the current medication and trims optional text.
     */
    private void applyValues(
            Medication medication,
            String name,
            String dose,
            String form,
            String instructions,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {
        medication.setName(TextNormalizer.strip(name));
        medication.setDose(TextNormalizer.stripToNull(dose));
        medication.setForm(TextNormalizer.stripToNull(form));
        medication.setInstructions(TextNormalizer.stripToNull(instructions));
        medication.setStartDate(startDate);
        medication.setEndDate(endDate);
        medication.setNotes(TextNormalizer.stripToNull(notes));
    }
}