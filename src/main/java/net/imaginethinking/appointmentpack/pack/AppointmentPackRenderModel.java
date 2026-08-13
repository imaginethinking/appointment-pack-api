package net.imaginethinking.appointmentpack.pack;

import java.util.List;

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

    public AppointmentPackRenderModel {
        medications = List.copyOf(medications);
        healthcareContacts = List.copyOf(healthcareContacts);
        emergencyContacts = List.copyOf(emergencyContacts);
        medicalHistory = List.copyOf(medicalHistory);
        bloodTests = List.copyOf(bloodTests);
    }

    public record PatientInformation(
            String fullName,
            String dateOfBirth,
            String nhsNumber,
            String chiNumber,
            String hcNumber,
            String bloodType,
            List<String> addressLines
    ) {

        public PatientInformation {
            addressLines = List.copyOf(addressLines);
        }
    }

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

        public AppointmentInformation {
            addressLines = List.copyOf(addressLines);
        }
    }

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

    public record HealthcareContactInformation(
            String name,
            String role,
            String organisation,
            String phoneNumber,
            String email,
            List<String> addressLines,
            String notes
    ) {

        public HealthcareContactInformation {
            addressLines = List.copyOf(addressLines);
        }
    }

    public record EmergencyContactInformation(
            String name,
            String relationship,
            String phoneNumber,
            String alternativePhoneNumber,
            String email,
            String notes
    ) {
    }

    public record MedicalHistoryInformation(
            String title,
            String entryDate,
            String summary
    ) {
    }

    public record BloodTestInformation(
            String title,
            String testDate,
            String provider,
            String notes,
            List<BloodResultInformation> results
    ) {

        public BloodTestInformation {
            results = List.copyOf(results);
        }
    }

    public record BloodResultInformation(
            String analyteName,
            String resultValue,
            String unit,
            String referenceRange,
            String flag
    ) {
    }
}