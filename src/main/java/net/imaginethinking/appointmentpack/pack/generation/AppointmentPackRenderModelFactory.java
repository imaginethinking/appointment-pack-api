package net.imaginethinking.appointmentpack.pack.generation;

import net.imaginethinking.appointmentpack.address.Address;
import net.imaginethinking.appointmentpack.appointment.Appointment;
import net.imaginethinking.appointmentpack.bloodtest.BloodTest;
import net.imaginethinking.appointmentpack.bloodtest.BloodTestResult;
import net.imaginethinking.appointmentpack.bloodtest.BloodTestResultFlag;
import net.imaginethinking.appointmentpack.common.TextNormalizer;
import net.imaginethinking.appointmentpack.contact.EmergencyContact;
import net.imaginethinking.appointmentpack.contact.HealthcareContact;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryEntry;
import net.imaginethinking.appointmentpack.medication.Medication;
import net.imaginethinking.appointmentpack.patientrecord.BloodType;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.profile.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Converts selected patient information into the values displayed in the Appointment Pack PDF.
 */
@Component
public class AppointmentPackRenderModelFactory {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.UK);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.UK);
    private static final ZoneId APPLICATION_ZONE = ZoneId.of("Europe/London");

    /**
     * Builds the complete model used by the PDF template from the selected patient resources.
     */
    public AppointmentPackRenderModel create(
            AppointmentPackSelection selection,
            String title,
            String notes,
            Instant generatedAt) {
        return new AppointmentPackRenderModel(
                title,
                formatGeneratedDate(generatedAt),
                toPatientInformation(selection.patientRecord()),
                toAppointmentInformation(selection.appointment()),
                notes,
                selection.medications().stream().map(this::toMedicationInformation).toList(),
                selection.healthcareContacts().stream().map(this::toHealthcareContactInformation).toList(),
                selection.emergencyContacts().stream().map(this::toEmergencyContactInformation).toList(),
                selection.medicalHistoryEntries().stream().map(this::toMedicalHistoryInformation).toList(),
                selection.bloodTests().stream().map(this::toBloodTestInformation).toList());
    }

    /**
     * Builds the patient details displayed at the start of the Appointment Pack.
     */
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

    /**
     * Builds the appointment details displayed in the Appointment Pack.
     */
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

    /**
     * Builds the medication details displayed in the Appointment Pack.
     */
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

    /**
     * Builds the healthcare contact details displayed in the Appointment Pack.
     */
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

    /**
     * Builds the emergency contact details displayed in the Appointment Pack.
     */
    private AppointmentPackRenderModel.EmergencyContactInformation toEmergencyContactInformation(EmergencyContact contact) {
        return new AppointmentPackRenderModel.EmergencyContactInformation(
                contact.getName(),
                contact.getRelationship(),
                contact.getPhoneNumber(),
                contact.getAlternativePhoneNumber(),
                contact.getEmail(),
                contact.getNotes());
    }

    /**
     * Builds the Medical History details displayed in the Appointment Pack.
     */
    private AppointmentPackRenderModel.MedicalHistoryInformation toMedicalHistoryInformation(MedicalHistoryEntry entry) {
        return new AppointmentPackRenderModel.MedicalHistoryInformation(
                entry.getTitle(),
                formatDate(entry.getEntryDate()),
                entry.getSummary());
    }

    /**
     * Builds a blood test and its result rows for the Appointment Pack.
     */
    private AppointmentPackRenderModel.BloodTestInformation toBloodTestInformation(BloodTest bloodTest) {
        List<AppointmentPackRenderModel.BloodResultInformation> results = bloodTest.getResults()
                .stream()
                .sorted(Comparator.comparingInt(BloodTestResult::getDisplayOrder))
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

    /**
     * Builds one blood result row for the Appointment Pack.
     */
    private AppointmentPackRenderModel.BloodResultInformation toBloodResultInformation(BloodTestResult result) {
        return new AppointmentPackRenderModel.BloodResultInformation(
                result.getAnalyteName(),
                result.getResultValue(),
                result.getUnit(),
                result.getReferenceRange(),
                formatFlag(result.getFlag()));
    }

    /**
     * Collects the non blank parts of an address in the order they should appear in the PDF.
     */
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

    /**
     * Adds a trimmed display value when it is not null or blank.
     */
    private void addIfPresent(List<String> values, String value) {
        String normalisedValue = TextNormalizer.stripToNull(value);

        if (normalisedValue != null) {
            values.add(normalisedValue);
        }
    }

    /**
     * Formats the pack generation time using the date format shown in the PDF.
     */
    private String formatGeneratedDate(Instant generatedAt) {
        return DATE_FORMATTER.format(generatedAt.atZone(APPLICATION_ZONE).toLocalDate());
    }

    /**
     * Formats an optional date for the PDF or returns an empty value when no date is available.
     */
    private String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }

        return DATE_FORMATTER.format(date);
    }

    /**
     * Formats an optional time for the PDF using hours and minutes.
     */
    private String formatTime(LocalTime time) {
        if (time == null) {
            return null;
        }

        return TIME_FORMATTER.format(time);
    }

    /**
     * Formats a blood type enum into the label shown in the PDF.
     */
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

    /**
     * Formats an optional blood result flag into the label shown in the PDF.
     */
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