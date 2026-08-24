package net.imaginethinking.appointmentpack.medication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.*;

class MedicationRequestValidationTest {

    @Test
    void shouldAcceptRepresentativeMedicationRequests() {
        MedicationRequests requests = requests(
                "Example Medication",
                "10 mg",
                "Tablet",
                "Take once daily",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1),
                "Example notes");

        assertBothValid(requests);
    }

    @Test
    void shouldEnforceMedicationNameLengthBoundary() {
        assertBothValid(withName(stringOfLength(199)));
        assertBothValid(withName(stringOfLength(200)));

        assertBothInvalidField(withName(stringOfLength(201)), "name");
    }

    @ParameterizedTest
    @MethodSource("invalidRequiredNames")
    void shouldRejectBlankOrNullMedicationName(String value) {
        assertBothInvalidField(withName(value), "name");
    }

    @ParameterizedTest(name = "{0}: {1} and {2} valid, {3} invalid")
    @MethodSource("optionalTextBoundaries")
    void shouldEnforceOptionalMedicationTextBoundaries(String field, int belowMaximum, int maximum, int aboveMaximum) {
        assertBothValid(withTextField(field, stringOfLength(belowMaximum)));
        assertBothValid(withTextField(field, stringOfLength(maximum)));

        assertBothInvalidField(withTextField(field, stringOfLength(aboveMaximum)), field);
    }

    @Test
    void shouldAcceptNullOptionalMedicationFields() {
        MedicationRequests requests = requests("Example Medication", null, null, null, null, null, null);

        assertBothValid(requests);
    }

    private static Stream<String> invalidRequiredNames() {
        return Stream.of("", "   ", null);
    }

    private static Stream<Arguments> optionalTextBoundaries() {
        return Stream.of(
                Arguments.of("dose", 99, 100, 101),
                Arguments.of("form", 99, 100, 101),
                Arguments.of("instructions", 499, 500, 501),
                Arguments.of("notes", 1999, 2000, 2001));
    }

    private static MedicationRequests withName(String name) {
        return requests(name, null, null, null, null, null, null);
    }

    private static MedicationRequests withTextField(String field, String value) {
        return switch (field) {
            case "dose" -> requests("Example Medication", value, null, null, null, null, null);
            case "form" -> requests("Example Medication", null, value, null, null, null, null);
            case "instructions" -> requests("Example Medication", null, null, value, null, null, null);
            case "notes" -> requests("Example Medication", null, null, null, null, null, value);
            default -> throw new IllegalArgumentException("Unsupported medication field: " + field);
        };
    }

    private static MedicationRequests requests(
            String name,
            String dose,
            String form,
            String instructions,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {
        return new MedicationRequests(
                new CreateMedicationRequest(name, dose, form, instructions, startDate, endDate, notes),
                new UpdateMedicationRequest(name, dose, form, instructions, startDate, endDate, notes));
    }

    private static void assertBothValid(MedicationRequests requests) {
        assertValid(requests.createRequest());
        assertValid(requests.updateRequest());
    }

    private static void assertBothInvalidField(MedicationRequests requests, String field) {
        assertInvalidField(requests.createRequest(), field);
        assertInvalidField(requests.updateRequest(), field);
    }

    private record MedicationRequests(CreateMedicationRequest createRequest, UpdateMedicationRequest updateRequest) {
    }
}