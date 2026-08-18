package net.imaginethinking.appointmentpack.pack.generation;

import net.imaginethinking.appointmentpack.address.Address;
import net.imaginethinking.appointmentpack.appointment.Appointment;
import net.imaginethinking.appointmentpack.bloodtest.BloodTest;
import net.imaginethinking.appointmentpack.bloodtest.BloodTestResult;
import net.imaginethinking.appointmentpack.bloodtest.BloodTestResultFlag;
import net.imaginethinking.appointmentpack.medication.Medication;
import net.imaginethinking.appointmentpack.patientrecord.BloodType;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.profile.Profile;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentPackRenderModelFactoryTest {

    private final AppointmentPackRenderModelFactory factory = new AppointmentPackRenderModelFactory();

    @Test
    void shouldMapPatientAndAppointmentInformationForPdfModel() {
        PatientRecord patientRecord = patientRecord();
        Appointment appointment = appointment(patientRecord);

        AppointmentPackSelection selection = new AppointmentPackSelection(
                patientRecord,
                appointment,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        AppointmentPackRenderModel model = factory.create(
                selection,
                "Neurology Appointment Pack",
                "Pack notes",
                Instant.parse("2026-08-14T12:00:00Z"));

        assertEquals("Neurology Appointment Pack", model.title());

        assertEquals("14 August 2026", model.generatedDate());

        assertEquals("Jane Patient", model.patient().fullName());

        assertEquals("2 January 1985", model.patient().dateOfBirth());

        assertEquals("1234567890", model.patient().nhsNumber());

        assertEquals("O+", model.patient().bloodType());

        assertEquals(List.of("1 Test Street", "Glasgow", "G1 1AA", "Scotland"), model.patient().addressLines());

        assertEquals("10 September 2026", model.appointment().date());

        assertEquals("10:00", model.appointment().startTime());

        assertEquals("10:30", model.appointment().endTime());

        assertEquals("Neurology", model.appointment().service());

        assertEquals("Pack notes", model.notes());
    }

    @Test
    void shouldMapSelectedMedicationAndBloodResults() {
        PatientRecord patientRecord = patientRecord();
        Appointment appointment = appointment(patientRecord);

        Medication medication = new Medication();

        ReflectionTestUtils.setField(medication, "id", UUID.randomUUID());

        medication.setPatientRecord(patientRecord);
        medication.setName("Amitriptyline");
        medication.setDose("10 mg");
        medication.setForm("Tablet");
        medication.setInstructions("At night");
        medication.setStartDate(LocalDate.of(2026, 1, 2));

        BloodTest bloodTest = new BloodTest();

        ReflectionTestUtils.setField(bloodTest, "id", UUID.randomUUID());

        bloodTest.setPatientRecord(patientRecord);
        bloodTest.setTitle(null);
        bloodTest.setTestDate(LocalDate.of(2026, 7, 5));

        bloodTest.setProvider("NHS Lab");

        BloodTestResult secondResult = bloodResult(bloodTest, "CRP", "2", "mg/L", BloodTestResultFlag.NORMAL, 1);

        BloodTestResult firstResult = bloodResult(bloodTest, "Haemoglobin", "142", "g/L", BloodTestResultFlag.HIGH, 0);

        bloodTest.getResults().add(secondResult);
        bloodTest.getResults().add(firstResult);

        AppointmentPackSelection selection = new AppointmentPackSelection(
                patientRecord,
                appointment,
                List.of(medication),
                List.of(),
                List.of(),
                List.of(),
                List.of(bloodTest));

        AppointmentPackRenderModel model = factory.create(
                selection,
                "Pack",
                null,
                Instant.parse("2026-08-14T12:00:00Z"));

        assertEquals(1, model.medications().size());

        assertEquals("Amitriptyline", model.medications().getFirst().name());

        assertEquals("2 January 2026", model.medications().getFirst().startDate());

        assertEquals(1, model.bloodTests().size());

        assertEquals("Blood test", model.bloodTests().getFirst().title());

        assertEquals(
                List.of("Haemoglobin", "CRP"),
                model.bloodTests()
                        .getFirst()
                        .results()
                        .stream()
                        .map(AppointmentPackRenderModel.BloodResultInformation::analyteName)
                        .toList());

        assertEquals("High", model.bloodTests().getFirst().results().getFirst().flag());
    }

    @Test
    void shouldProduceEmptyListsWhenNoOptionalResourcesAreSelected() {
        PatientRecord patientRecord = patientRecord();

        AppointmentPackRenderModel model = factory.create(
                new AppointmentPackSelection(
                        patientRecord,
                        appointment(patientRecord),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()), "Pack", null, Instant.parse("2026-08-14T12:00:00Z"));

        assertTrue(model.medications().isEmpty());
        assertTrue(model.healthcareContacts().isEmpty());
        assertTrue(model.emergencyContacts().isEmpty());
        assertTrue(model.medicalHistory().isEmpty());
        assertTrue(model.bloodTests().isEmpty());
    }

    private BloodTestResult bloodResult(
            BloodTest bloodTest,
            String analyteName,
            String resultValue,
            String unit,
            BloodTestResultFlag flag,
            int displayOrder) {
        BloodTestResult result = new BloodTestResult();

        result.setBloodTest(bloodTest);
        result.setAnalyteName(analyteName);
        result.setAnalyteKey(analyteName.toLowerCase());
        result.setResultValue(resultValue);
        result.setUnit(unit);
        result.setFlag(flag);
        result.setDisplayOrder(displayOrder);

        return result;
    }

    private PatientRecord patientRecord() {
        Profile profile = new Profile();

        profile.setFirstName(" Jane ");
        profile.setLastName(" Patient ");
        profile.setDateOfBirth(LocalDate.of(1985, 1, 2));
        profile.setAddress(address());

        PatientRecord patientRecord = new PatientRecord();

        ReflectionTestUtils.setField(patientRecord, "id", UUID.randomUUID());

        patientRecord.setProfile(profile);
        patientRecord.setNhsNumber(" 1234567890 ");
        patientRecord.setBloodType(BloodType.O_POSITIVE);

        return patientRecord;
    }

    private Appointment appointment(
            PatientRecord patientRecord) {
        Appointment appointment = new Appointment();

        ReflectionTestUtils.setField(appointment, "id", UUID.randomUUID());

        appointment.setPatientRecord(patientRecord);
        appointment.setDate(LocalDate.of(2026, 9, 10));
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 30));
        appointment.setService(" Neurology ");
        appointment.setAppointmentType(" Follow-up ");
        appointment.setClinicianOrTeam(" Dr Smith ");
        appointment.setLocationName(" Clinic A ");
        appointment.setAddress(address());
        appointment.setNotes(" Bring medication list ");

        return appointment;
    }

    private Address address() {
        Address address = new Address();

        address.setAddressLine1(" 1 Test Street ");
        address.setTownCity(" Glasgow ");
        address.setPostcode(" G1 1AA ");
        address.setCountry(" Scotland ");

        return address;
    }
}