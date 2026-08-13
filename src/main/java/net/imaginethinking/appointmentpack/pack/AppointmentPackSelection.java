package net.imaginethinking.appointmentpack.pack;

import net.imaginethinking.appointmentpack.appointment.Appointment;
import net.imaginethinking.appointmentpack.bloodtest.BloodTest;
import net.imaginethinking.appointmentpack.contact.EmergencyContact;
import net.imaginethinking.appointmentpack.contact.HealthcareContact;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryEntry;
import net.imaginethinking.appointmentpack.medication.Medication;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;

import java.util.ArrayList;
import java.util.List;

public record AppointmentPackSelection(
        PatientRecord patientRecord,
        Appointment appointment,
        List<Medication> medications,
        List<HealthcareContact> healthcareContacts,
        List<EmergencyContact> emergencyContacts,
        List<MedicalHistoryEntry> medicalHistoryEntries,
        List<BloodTest> bloodTests
) {

    public AppointmentPackSelection {
        medications = List.copyOf(medications);
        healthcareContacts = List.copyOf(healthcareContacts);
        emergencyContacts = List.copyOf(emergencyContacts);
        medicalHistoryEntries = List.copyOf(medicalHistoryEntries);
        bloodTests = List.copyOf(bloodTests);
    }

    public List<AppointmentPackGenerationData.SelectedItem> selectedItems() {
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

        for (MedicalHistoryEntry entry : medicalHistoryEntries) {
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
}