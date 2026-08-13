package net.imaginethinking.appointmentpack.pack;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.address.Address;
import net.imaginethinking.appointmentpack.appointment.Appointment;
import net.imaginethinking.appointmentpack.appointment.AppointmentPermission;
import net.imaginethinking.appointmentpack.appointment.AppointmentRepository;
import net.imaginethinking.appointmentpack.bloodtest.*;
import net.imaginethinking.appointmentpack.contact.*;
import net.imaginethinking.appointmentpack.common.TextNormalizer;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryEntry;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryEntryRepository;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryPermission;
import net.imaginethinking.appointmentpack.medication.Medication;
import net.imaginethinking.appointmentpack.medication.MedicationPermission;
import net.imaginethinking.appointmentpack.medication.MedicationRepository;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import net.imaginethinking.appointmentpack.patientrecord.bloodtype.BloodType;
import net.imaginethinking.appointmentpack.profile.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentPackGenerationDataService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.UK);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.UK);

    private static final ZoneId APPLICATION_ZONE = ZoneId.of("Europe/London");

    private final AppointmentRepository appointmentRepository;
    private final MedicationRepository medicationRepository;
    private final HealthcareContactRepository healthcareContactRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final MedicalHistoryEntryRepository medicalHistoryEntryRepository;
    private final BloodTestRepository bloodTestRepository;
    private final PatientRecordAccessService patientRecordAccessService;

    @Transactional(readOnly = true)
    public AppointmentPackGenerationData prepare(
            UUID authenticatedUserId,
            UUID patientRecordId,
            AppointmentPackGenerationRequest request) {
        PatientRecord patientRecord = patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecordId,
                AppointmentPackPermission.CREATE);

        Appointment appointment = findAppointmentForPatient(patientRecordId, request.appointmentId());

        patientRecordAccessService.requireAccess(authenticatedUserId, patientRecord, AppointmentPermission.VIEW);

        List<Medication> medications = loadMedications(authenticatedUserId, patientRecord, request.medicationIds());

        List<HealthcareContact> healthcareContacts = loadHealthcareContacts(
                authenticatedUserId,
                patientRecord,
                request.healthcareContactIds());

        List<EmergencyContact> emergencyContacts = loadEmergencyContacts(
                authenticatedUserId,
                patientRecord,
                request.emergencyContactIds());

        List<MedicalHistoryEntry> medicalHistory = loadMedicalHistory(
                authenticatedUserId,
                patientRecord,
                request.medicalHistoryEntryIds());

        List<BloodTest> bloodTests = loadBloodTests(authenticatedUserId, patientRecord, request.bloodTestIds());

        Instant generatedAt = Instant.now();

        String title = resolveTitle(request.title(), appointment);

        String notes = TextNormalizer.stripToNull(request.notes());

        AppointmentPackRenderModel renderModel = buildRenderModel(
                patientRecord,
                appointment,
                title,
                notes,
                generatedAt,
                medications,
                healthcareContacts,
                emergencyContacts,
                medicalHistory,
                bloodTests);

        List<AppointmentPackGenerationData.SelectedItem> selectedItems = buildSelectedItems(
                medications,
                healthcareContacts,
                emergencyContacts,
                medicalHistory,
                bloodTests);

        return new AppointmentPackGenerationData(
                patientRecordId,
                appointment.getId(),
                title,
                notes,
                generatedAt,
                createFileName(title),
                renderModel,
                selectedItems);
    }

    private Appointment findAppointmentForPatient(UUID patientRecordId, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (!appointment.getPatientRecord().getId().equals(patientRecordId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found");
        }

        if (appointment.isArchived()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found");
        }

        return appointment;
    }

    private List<Medication> loadMedications(
            UUID authenticatedUserId,
            PatientRecord patientRecord,
            List<UUID> selectedIds) {
        if (selectedIds.isEmpty()) {
            return List.of();
        }

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecord,
                MedicationPermission.VIEW);

        return resolveSelection(
                selectedIds,
                medicationRepository.findAllById(selectedIds),
                Medication::getId,
                medication -> !medication.isArchived() && medication.getPatientRecord()
                        .getId()
                        .equals(patientRecord.getId()),
                "medications");
    }

    private List<HealthcareContact> loadHealthcareContacts(
            UUID authenticatedUserId,
            PatientRecord patientRecord,
            List<UUID> selectedIds) {
        if (selectedIds.isEmpty()) {
            return List.of();
        }

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecord,
                ContactPermission.VIEW);

        return resolveSelection(
                selectedIds,
                healthcareContactRepository.findAllById(selectedIds),
                HealthcareContact::getId,
                contact -> !contact.isArchived() && contact.getPatientRecord().getId().equals(patientRecord.getId()),
                "healthcare contacts");
    }

    private List<EmergencyContact> loadEmergencyContacts(
            UUID authenticatedUserId,
            PatientRecord patientRecord,
            List<UUID> selectedIds) {
        if (selectedIds.isEmpty()) {
            return List.of();
        }

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecord,
                ContactPermission.VIEW);

        return resolveSelection(
                selectedIds,
                emergencyContactRepository.findAllById(selectedIds),
                EmergencyContact::getId,
                contact -> !contact.isArchived() && contact.getPatientRecord().getId().equals(patientRecord.getId()),
                "emergency contacts");
    }

    private List<MedicalHistoryEntry> loadMedicalHistory(
            UUID authenticatedUserId,
            PatientRecord patientRecord,
            List<UUID> selectedIds) {
        if (selectedIds.isEmpty()) {
            return List.of();
        }

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecord,
                MedicalHistoryPermission.VIEW);

        return resolveSelection(
                selectedIds,
                medicalHistoryEntryRepository.findAllById(selectedIds),
                MedicalHistoryEntry::getId,
                entry -> !entry.isArchived() && entry.getPatientRecord().getId().equals(patientRecord.getId()),
                "medical-history entries");
    }

    private List<BloodTest> loadBloodTests(
            UUID authenticatedUserId,
            PatientRecord patientRecord,
            List<UUID> selectedIds) {
        if (selectedIds.isEmpty()) {
            return List.of();
        }

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecord,
                BloodTestPermission.VIEW);

        return resolveSelection(
                selectedIds,
                bloodTestRepository.findAllById(selectedIds),
                BloodTest::getId,
                bloodTest -> !bloodTest.isArchived() && bloodTest.getPatientRecord()
                        .getId()
                        .equals(patientRecord.getId()),
                "blood tests");
    }

    private <T> List<T> resolveSelection(
            List<UUID> selectedIds,
            List<T> loadedValues,
            Function<T, UUID> idExtractor,
            Predicate<T> validSelection,
            String resourceName) {
        ensureUniqueSelection(selectedIds, resourceName);

        Map<UUID, T> valuesById = loadedValues.stream().collect(Collectors.toMap(idExtractor, Function.identity()));

        List<T> resolved = new ArrayList<>();

        for (UUID selectedId : selectedIds) {
            T value = valuesById.get(selectedId);

            if (value == null || !validSelection.test(value)) {
                throw invalidSelection(resourceName);
            }

            resolved.add(value);
        }

        return List.copyOf(resolved);
    }

    private void ensureUniqueSelection(List<UUID> selectedIds, String resourceName) {
        Set<UUID> uniqueIds = new HashSet<>(selectedIds);

        if (uniqueIds.size() != selectedIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate " + resourceName + " were selected");
        }
    }

    private ResponseStatusException invalidSelection(
            String resourceName) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "One or more selected " + resourceName + " are unavailable");
    }

    private AppointmentPackRenderModel buildRenderModel(
            PatientRecord patientRecord,
            Appointment appointment,
            String title,
            String notes,
            Instant generatedAt,
            List<Medication> medications,
            List<HealthcareContact> healthcareContacts,
            List<EmergencyContact> emergencyContacts,
            List<MedicalHistoryEntry> medicalHistory,
            List<BloodTest> bloodTests) {
        return new AppointmentPackRenderModel(
                title,
                DATE_FORMATTER.format(generatedAt.atZone(APPLICATION_ZONE).toLocalDate()),
                toPatientInformation(patientRecord),
                toAppointmentInformation(appointment),
                notes,
                medications.stream().map(this::toMedicationInformation).toList(),
                healthcareContacts.stream().map(this::toHealthcareContactInformation).toList(),
                emergencyContacts.stream().map(this::toEmergencyContactInformation).toList(),
                medicalHistory.stream().map(this::toMedicalHistoryInformation).toList(),
                bloodTests.stream().map(this::toBloodTestInformation).toList());
    }

    private AppointmentPackRenderModel.PatientInformation toPatientInformation(PatientRecord patientRecord) {
        Profile profile = patientRecord.getProfile();

        return new AppointmentPackRenderModel.PatientInformation(
                TextNormalizer.strip(profile.getFirstName()) + " " + TextNormalizer.strip(profile.getLastName()),
                formatDate(profile.getDateOfBirth()),
                TextNormalizer.stripToNull(patientRecord.getNhsNumber()),
                TextNormalizer.stripToNull(patientRecord.getChiNumber()),
                TextNormalizer.stripToNull(patientRecord.getHcNumber()),
                formatBloodType(patientRecord.getBloodType()),
                toAddressLines(profile.getAddress()));
    }

    private AppointmentPackRenderModel.AppointmentInformation toAppointmentInformation(Appointment appointment) {
        return new AppointmentPackRenderModel.AppointmentInformation(
                formatDate(appointment.getDate()),
                formatTime(appointment.getStartTime()),
                formatTime(appointment.getEndTime()),
                TextNormalizer.stripToNull(appointment.getService()),
                TextNormalizer.stripToNull(appointment.getAppointmentType()),
                TextNormalizer.stripToNull(appointment.getClinicianOrTeam()),
                TextNormalizer.stripToNull(appointment.getLocationName()),
                toAddressLines(appointment.getAddress()),
                TextNormalizer.stripToNull(appointment.getNotes()));
    }

    private AppointmentPackRenderModel.MedicationInformation toMedicationInformation(Medication medication) {
        return new AppointmentPackRenderModel.MedicationInformation(
                medication.getName(),
                medication.getDose(),
                medication.getForm(),
                medication.getInstructions(),
                formatDate(medication.getStartDate()),
                formatDate(medication.getEndDate()),
                medication.getNotes());
    }

    private AppointmentPackRenderModel.HealthcareContactInformation toHealthcareContactInformation(HealthcareContact contact) {
        return new AppointmentPackRenderModel.HealthcareContactInformation(
                contact.getName(),
                contact.getRole(),
                contact.getOrganisation(),
                contact.getPhoneNumber(),
                contact.getEmail(),
                toAddressLines(contact.getAddress()),
                contact.getNotes());
    }

    private AppointmentPackRenderModel.EmergencyContactInformation toEmergencyContactInformation(EmergencyContact contact) {
        return new AppointmentPackRenderModel.EmergencyContactInformation(
                contact.getName(),
                contact.getRelationship(),
                contact.getPhoneNumber(),
                contact.getAlternativePhoneNumber(),
                contact.getEmail(),
                contact.getNotes());
    }

    private AppointmentPackRenderModel.MedicalHistoryInformation toMedicalHistoryInformation(MedicalHistoryEntry entry) {
        return new AppointmentPackRenderModel.MedicalHistoryInformation(
                entry.getTitle(),
                formatDate(entry.getEntryDate()),
                entry.getSummary());
    }

    private AppointmentPackRenderModel.BloodTestInformation toBloodTestInformation(BloodTest bloodTest) {
        List<AppointmentPackRenderModel.BloodResultInformation> results = bloodTest.getResults()
                .stream()
                .sorted(java.util.Comparator.comparingInt(BloodTestResult::getDisplayOrder))
                .map(this::toBloodResultInformation)
                .toList();

        String title = TextNormalizer.stripToNull(bloodTest.getTitle());

        if (title == null) {
            title = "Blood test";
        }

        return new AppointmentPackRenderModel.BloodTestInformation(
                title,
                formatDate(bloodTest.getTestDate()),
                bloodTest.getProvider(),
                bloodTest.getNotes(),
                results);
    }

    private AppointmentPackRenderModel.BloodResultInformation toBloodResultInformation(BloodTestResult result) {
        return new AppointmentPackRenderModel.BloodResultInformation(
                result.getAnalyteName(),
                result.getResultValue(),
                result.getUnit(),
                result.getReferenceRange(),
                formatFlag(result.getFlag()));
    }

    private List<AppointmentPackGenerationData.SelectedItem> buildSelectedItems(
            List<Medication> medications,
            List<HealthcareContact> healthcareContacts,
            List<EmergencyContact> emergencyContacts,
            List<MedicalHistoryEntry> medicalHistory,
            List<BloodTest> bloodTests) {
        List<AppointmentPackGenerationData.SelectedItem> items = new ArrayList<>();

        int displayOrder = 0;

        for (Medication medication : medications) {
            items.add(new AppointmentPackGenerationData.SelectedItem(
                    AppointmentPackItemType.MEDICATION,
                    medication.getId(),
                    displayOrder++));
        }

        for (HealthcareContact contact : healthcareContacts) {
            items.add(new AppointmentPackGenerationData.SelectedItem(
                    AppointmentPackItemType.HEALTHCARE_CONTACT,
                    contact.getId(),
                    displayOrder++));
        }

        for (EmergencyContact contact : emergencyContacts) {
            items.add(new AppointmentPackGenerationData.SelectedItem(
                    AppointmentPackItemType.EMERGENCY_CONTACT,
                    contact.getId(),
                    displayOrder++));
        }

        for (MedicalHistoryEntry entry : medicalHistory) {
            items.add(new AppointmentPackGenerationData.SelectedItem(
                    AppointmentPackItemType.MEDICAL_HISTORY,
                    entry.getId(),
                    displayOrder++));
        }

        for (BloodTest bloodTest : bloodTests) {
            items.add(new AppointmentPackGenerationData.SelectedItem(
                    AppointmentPackItemType.BLOOD_TEST,
                    bloodTest.getId(),
                    displayOrder++));
        }

        return List.copyOf(items);
    }

    private String resolveTitle(String requestedTitle, Appointment appointment) {
        String title = TextNormalizer.stripToNull(requestedTitle);

        if (title != null) {
            return title;
        }

        String service = TextNormalizer.stripToNull(appointment.getService());

        String defaultTitle = service == null ? "Appointment Pack" : service + " Appointment Pack";

        defaultTitle += " – " + formatDate(appointment.getDate());

        if (defaultTitle.length() > 250) {
            return TextNormalizer.strip(defaultTitle.substring(0, 250));
        }

        return defaultTitle;
    }

    private String createFileName(String title) {
        String normalisedTitle = Normalizer.normalize(title, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .toLowerCase(Locale.ROOT);

        if (normalisedTitle.isBlank()) {
            normalisedTitle = "appointment-pack";
        }

        if (normalisedTitle.length() > 120) {
            normalisedTitle = normalisedTitle.substring(0, 120);
        }

        return normalisedTitle + ".pdf";
    }

    private List<String> toAddressLines(Address address) {
        if (address == null) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();

        addIfPresent(lines, address.getAddressLine1());
        addIfPresent(lines, address.getAddressLine2());
        addIfPresent(lines, address.getTownCity());
        addIfPresent(lines, address.getCounty());
        addIfPresent(lines, address.getPostcode());
        addIfPresent(lines, address.getCountry());

        return List.copyOf(lines);
    }

    private void addIfPresent(List<String> values, String value) {
        String normalisedValue = TextNormalizer.stripToNull(value);

        if (normalisedValue != null) {
            values.add(normalisedValue);
        }
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }

        return DATE_FORMATTER.format(date);
    }

    private String formatTime(LocalTime time) {
        if (time == null) {
            return null;
        }

        return TIME_FORMATTER.format(time);
    }

    private String formatBloodType(BloodType bloodType) {
        if (bloodType == null) {
            return null;
        }

        return switch (bloodType) {
            case A_POSITIVE -> "A+";
            case A_NEGATIVE -> "A-";
            case B_POSITIVE -> "B+";
            case B_NEGATIVE -> "B-";
            case AB_POSITIVE -> "AB+";
            case AB_NEGATIVE -> "AB-";
            case O_POSITIVE -> "O+";
            case O_NEGATIVE -> "O-";
        };
    }

    private String formatFlag(BloodTestResultFlag flag) {
        if (flag == null) {
            return null;
        }

        return switch (flag) {
            case LOW -> "Low";
            case NORMAL -> "Normal";
            case HIGH -> "High";
            case ABNORMAL -> "Abnormal";
        };
    }

}