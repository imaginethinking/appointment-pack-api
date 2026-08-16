package net.imaginethinking.appointmentpack.document.processing.context;

import net.imaginethinking.appointmentpack.address.Address;
import net.imaginethinking.appointmentpack.document.Document;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.profile.Profile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RedactionContextFactoryTest {

    private final RedactionContextFactory factory = new RedactionContextFactory();

    @Test
    void shouldIncludeKnownPatientIdentifiersAndCommonFormattingVariants() {
        Document document = documentWithPatientDetails();

        RedactionContext context = factory.create(document);

        List<String> values = context.knownValues();

        assertTrue(values.contains("Jane"));
        assertTrue(values.contains("Patient"));
        assertTrue(values.contains("Jane Patient"));
        assertTrue(values.contains("Patient, Jane"));

        assertTrue(values.contains("1985-01-02"));
        assertTrue(values.contains("02/01/1985"));
        assertTrue(values.contains("02-01-1985"));
        assertTrue(values.contains("02.01.1985"));
        assertTrue(values.contains("2 January 1985"));
        assertTrue(values.contains("2 Jan 1985"));

        assertTrue(values.contains("123 456 7890"));
        assertTrue(values.contains("1234567890"));
        assertTrue(values.contains("123456 7890"));

        assertTrue(values.contains("1 Test Street"));
        assertTrue(values.contains("Glasgow"));
        assertTrue(values.contains("G1 1AA"));
        assertTrue(values.contains("G11AA"));
        assertTrue(values.contains("1 Test Street, Glasgow, G1 1AA"));
        assertTrue(values.contains("1 Test Street Glasgow G1 1AA"));
    }

    @Test
    void shouldTrimValuesAndRemoveDuplicates() {
        Document document = new Document();
        PatientRecord patientRecord = new PatientRecord();
        Profile profile = new Profile();

        profile.setFirstName(" Jane ");
        profile.setLastName(" Jane ");
        profile.setDateOfBirth(LocalDate.of(1985, 1, 2));

        patientRecord.setProfile(profile);
        patientRecord.setNhsNumber(" 1234567890 ");
        patientRecord.setChiNumber("1234567890");

        document.setPatientRecord(patientRecord);

        RedactionContext context = factory.create(document);

        assertEquals(1, context.knownValues().stream().filter("Jane"::equals).count());

        assertEquals(1, context.knownValues().stream().filter("1234567890"::equals).count());

        assertFalse(context.knownValues().stream().anyMatch(value -> !value.equals(value.strip())));
    }

    @Test
    void shouldIgnoreMissingOptionalPatientDetails() {
        Document document = new Document();
        PatientRecord patientRecord = new PatientRecord();
        Profile profile = new Profile();

        profile.setFirstName("Jane");
        profile.setLastName("Patient");
        profile.setDateOfBirth(null);
        profile.setAddress(null);

        patientRecord.setProfile(profile);
        patientRecord.setNhsNumber(null);
        patientRecord.setChiNumber("   ");
        patientRecord.setHcNumber(null);

        document.setPatientRecord(patientRecord);

        RedactionContext context = factory.create(document);

        assertEquals(List.of("Jane", "Patient", "Jane Patient", "Patient, Jane"), context.knownValues());
    }

    private Document documentWithPatientDetails() {
        Address address = new Address();
        address.setAddressLine1(" 1 Test Street ");
        address.setTownCity(" Glasgow ");
        address.setPostcode(" G1 1AA ");

        Profile profile = new Profile();
        profile.setFirstName(" Jane ");
        profile.setLastName(" Patient ");
        profile.setDateOfBirth(LocalDate.of(1985, 1, 2));
        profile.setAddress(address);

        PatientRecord patientRecord = new PatientRecord();
        patientRecord.setProfile(profile);
        patientRecord.setNhsNumber("123 456 7890");

        Document document = new Document();
        document.setPatientRecord(patientRecord);

        return document;
    }
}