package net.imaginethinking.appointmentpack.pack.generation;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.appointment.Appointment;
import net.imaginethinking.appointmentpack.appointment.AppointmentPermission;
import net.imaginethinking.appointmentpack.appointment.AppointmentRepository;
import net.imaginethinking.appointmentpack.bloodtest.BloodTest;
import net.imaginethinking.appointmentpack.bloodtest.BloodTestPermission;
import net.imaginethinking.appointmentpack.bloodtest.BloodTestRepository;
import net.imaginethinking.appointmentpack.contact.*;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryEntry;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryEntryRepository;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryPermission;
import net.imaginethinking.appointmentpack.medication.Medication;
import net.imaginethinking.appointmentpack.medication.MedicationPermission;
import net.imaginethinking.appointmentpack.medication.MedicationRepository;
import net.imaginethinking.appointmentpack.pack.AppointmentPackGenerationRequest;
import net.imaginethinking.appointmentpack.pack.AppointmentPackPermission;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Checks the requested Appointment Pack items and loads only resources that belong to the selected patient.
 */
@Service
@RequiredArgsConstructor
public class AppointmentPackSelectionService {

    private final AppointmentRepository appointmentRepository;
    private final MedicationRepository medicationRepository;
    private final HealthcareContactRepository healthcareContactRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final MedicalHistoryEntryRepository medicalHistoryEntryRepository;
    private final BloodTestRepository bloodTestRepository;
    private final PatientRecordAccessService patientRecordAccessService;

    /**
     * Checks pack creation access, loads the selected appointment and resources, then confirms every item belongs
     * to the same patient.
     */
    @Transactional(readOnly = true)
    public AppointmentPackSelection select(
            UUID authenticatedUserId,
            UUID patientRecordId,
            AppointmentPackGenerationRequest request) {
        PatientRecord patientRecord = patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecordId,
                AppointmentPackPermission.CREATE);

        Appointment appointment = requireAppointment(patientRecordId, request.appointmentId());

        requireSelectionPermissions(authenticatedUserId, patientRecord, request);

        List<Medication> medications = resolveSelection(
                request.medicationIds(),
                medicationRepository::findAllById,
                Medication::getId,
                medication -> belongsToPatient(
                        medication.getPatientRecord(),
                        patientRecord) && !medication.isArchived(),
                "medications");

        List<HealthcareContact> healthcareContacts = resolveSelection(
                request.healthcareContactIds(),
                healthcareContactRepository::findAllById,
                HealthcareContact::getId,
                contact -> belongsToPatient(contact.getPatientRecord(), patientRecord) && !contact.isArchived(),
                "healthcare contacts");

        List<EmergencyContact> emergencyContacts = resolveSelection(
                request.emergencyContactIds(),
                emergencyContactRepository::findAllById,
                EmergencyContact::getId,
                contact -> belongsToPatient(contact.getPatientRecord(), patientRecord) && !contact.isArchived(),
                "emergency contacts");

        List<MedicalHistoryEntry> medicalHistoryEntries = resolveSelection(
                request.medicalHistoryEntryIds(),
                medicalHistoryEntryRepository::findAllById,
                MedicalHistoryEntry::getId,
                entry -> belongsToPatient(entry.getPatientRecord(), patientRecord) && !entry.isArchived(),
                "medical-history entries");

        List<BloodTest> bloodTests = resolveSelection(
                request.bloodTestIds(),
                bloodTestRepository::findAllById,
                BloodTest::getId,
                bloodTest -> belongsToPatient(bloodTest.getPatientRecord(), patientRecord) && !bloodTest.isArchived(),
                "blood tests");

        return new AppointmentPackSelection(
                patientRecord,
                appointment,
                medications,
                healthcareContacts,
                emergencyContacts,
                medicalHistoryEntries,
                bloodTests);
    }

    /**
     * Loads the selected active appointment and checks that it belongs to the patient used for the pack.
     */
    private Appointment requireAppointment(UUID patientRecordId, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Appointment not found")
                );

        if (!appointment.getPatientRecord().getId().equals(patientRecordId) || appointment.isArchived()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Appointment not found"
            );
        }

        return appointment;
    }

    /**
     * Checks view permission for each type of patient information included in the pack request.
     */
    private void requireSelectionPermissions(
            UUID authenticatedUserId,
            PatientRecord patientRecord,
            AppointmentPackGenerationRequest request) {
        patientRecordAccessService.requireAccess(authenticatedUserId, patientRecord, AppointmentPermission.VIEW);

        if (!request.medicationIds().isEmpty()) {
            patientRecordAccessService.requireAccess(authenticatedUserId, patientRecord, MedicationPermission.VIEW);
        }

        if (!request.healthcareContactIds().isEmpty() || !request.emergencyContactIds().isEmpty()) {
            patientRecordAccessService.requireAccess(authenticatedUserId, patientRecord, ContactPermission.VIEW);
        }

        if (!request.medicalHistoryEntryIds().isEmpty()) {
            patientRecordAccessService.requireAccess(authenticatedUserId, patientRecord, MedicalHistoryPermission.VIEW);
        }

        if (!request.bloodTestIds().isEmpty()) {
            patientRecordAccessService.requireAccess(authenticatedUserId, patientRecord, BloodTestPermission.VIEW);
        }
    }

    /**
     * Rejects duplicate or missing IDs, loads the requested resources and checks that every selected item is valid
     * for the patient.
     */
    private <T> List<T> resolveSelection(
            List<UUID> selectedIds,
            Function<Iterable<UUID>, List<T>> loader,
            Function<T, UUID> idExtractor,
            Predicate<T> validSelection,
            String resourceName) {
        if (selectedIds.isEmpty()) {
            return List.of();
        }

        ensureUniqueSelection(selectedIds, resourceName);

        List<T> loadedValues = loader.apply(selectedIds);

        Map<UUID, T> valuesById = loadedValues.stream().collect(Collectors.toMap(idExtractor, Function.identity()));

        List<T> resolvedValues = new ArrayList<>();

        for (UUID selectedId : selectedIds) {
            T value = valuesById.get(selectedId);

            if (value == null || !validSelection.test(value)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "One or more selected " + resourceName + " are unavailable"
                );
            }

            resolvedValues.add(value);
        }

        return List.copyOf(resolvedValues);
    }

    /**
     * Rejects a pack selection when the same resource ID has been supplied more than once.
     */
    private void ensureUniqueSelection(List<UUID> selectedIds, String resourceName) {
        Set<UUID> uniqueIds = new HashSet<>(selectedIds);

        if (uniqueIds.size() != selectedIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duplicate " + resourceName + " were selected"
            );
        }
    }

    /**
     * Checks that a selected resource belongs to the patient used to generate the pack.
     */
    private boolean belongsToPatient(PatientRecord resourcePatientRecord, PatientRecord selectedPatientRecord) {
        return resourcePatientRecord.getId().equals(selectedPatientRecord.getId());
    }
}