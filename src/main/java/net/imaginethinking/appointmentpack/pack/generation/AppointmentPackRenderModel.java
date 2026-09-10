package net.imaginethinking.appointmentpack.pack.generation;

import java.util.List;

/**
 * Keeps the values passed into the Appointment Pack PDF template.
 */
public record AppointmentPackRenderModel(
        String title,
        String generatedDate,
        PatientInformation patient,
        AppointmentInformation appointment,
        String notes,
        List<MedicationInformation> medications,
        List<HealthcareContactInformation> healthcareContacts,
        List<EmergencyContactInformation> emergencyContacts,
        List<MedicalHistoryInformation> medicalHistory,
        List<BloodTestInformation> bloodTests
) {

    /**
     * Copies the resource lists used by the template so the render model stays unchanged while the PDF is
     * generated.
     */
    public AppointmentPackRenderModel {
        medications = List.copyOf(medications);
        healthcareContacts = List.copyOf(healthcareContacts);
        emergencyContacts = List.copyOf(emergencyContacts);
        medicalHistory = List.copyOf(medicalHistory);
        bloodTests = List.copyOf(bloodTests);
    }

    /**
     * Keeps the patient details displayed in an Appointment Pack.
     */
    public record PatientInformation(
            String fullName,
            String dateOfBirth,
            String nhsNumber,
            String chiNumber,
            String hcNumber,
            String bloodType,
            List<String> addressLines
    ) {

        /**
         * Creates the patient information with the supplied values.
         */
        public PatientInformation {
            addressLines = List.copyOf(addressLines);
        }
    }

    /**
     * Keeps the appointment details displayed in an Appointment Pack.
     */
    public record AppointmentInformation(
            String date,
            String startTime,
            String endTime,
            String service,
            String appointmentType,
            String clinicianOrTeam,
            String locationName,
            List<String> addressLines,
            String notes
    ) {

        /**
         * Creates the appointment information with the supplied values.
         */
        public AppointmentInformation {
            addressLines = List.copyOf(addressLines);
        }
    }

    /**
     * Keeps one medication displayed in an Appointment Pack.
     */
    public record MedicationInformation(
            String name,
            String dose,
            String form,
            String instructions,
            String startDate,
            String endDate,
            String notes
    ) {
    }

    /**
     * Keeps one healthcare contact displayed in an Appointment Pack.
     */
    public record HealthcareContactInformation(
            String name,
            String role,
            String organisation,
            String phoneNumber,
            String email,
            List<String> addressLines,
            String notes
    ) {

        /**
         * Creates the healthcare contact information with the supplied values.
         */
        public HealthcareContactInformation {
            addressLines = List.copyOf(addressLines);
        }
    }

    /**
     * Keeps one emergency contact displayed in an Appointment Pack.
     */
    public record EmergencyContactInformation(
            String name,
            String relationship,
            String phoneNumber,
            String alternativePhoneNumber,
            String email,
            String notes
    ) {
    }

    /**
     * Keeps one Medical History entry displayed in an Appointment Pack.
     */
    public record MedicalHistoryInformation(
            String title,
            String entryDate,
            String summary
    ) {
    }

    /**
     * Keeps one blood test and its result rows displayed in an Appointment Pack.
     */
    public record BloodTestInformation(
            String title,
            String testDate,
            String provider,
            String notes,
            List<BloodResultInformation> results
    ) {

        /**
         * Creates the blood test information with the supplied values.
         */
        public BloodTestInformation {
            results = List.copyOf(results);
        }
    }

    /**
     * Keeps one blood result row displayed in an Appointment Pack.
     */
    public record BloodResultInformation(
            String analyteName,
            String resultValue,
            String unit,
            String referenceRange,
            String flag
    ) {
    }
}