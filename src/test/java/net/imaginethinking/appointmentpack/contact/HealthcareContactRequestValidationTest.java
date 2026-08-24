package net.imaginethinking.appointmentpack.contact;

import net.imaginethinking.appointmentpack.address.AddressRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.*;

class HealthcareContactRequestValidationTest {

    @Test
    void shouldAcceptRepresentativeHealthcareContactRequests() {
        HealthcareContactRequests requests = requests(
                "Example Clinician",
                "Consultant",
                "Example Health Service",
                "01234 567890",
                "clinician@example.com",
                new AddressRequest("1 Example Road", null, "Exampletown", null, "AB1 2CD", "United Kingdom"),
                "Primary healthcare contact");

        assertBothValid(requests);
    }

    @Test
    void shouldEnforceHealthcareContactNameBoundary() {
        assertBothValid(withName(stringOfLength(199)));
        assertBothValid(withName(stringOfLength(200)));

        assertBothInvalidField(withName(stringOfLength(201)), "name");
    }

    @ParameterizedTest
    @MethodSource("invalidRequiredNames")
    void shouldRejectBlankOrNullHealthcareContactName(String name) {
        assertBothInvalidField(withName(name), "name");
    }

    @ParameterizedTest(name = "{0}: {1} and {2} valid, {3} invalid")
    @MethodSource("optionalTextBoundaries")
    void shouldEnforceHealthcareContactTextBoundaries(String field, int belowMaximum, int maximum, int aboveMaximum) {
        assertBothValid(withTextField(field, stringOfLength(belowMaximum)));
        assertBothValid(withTextField(field, stringOfLength(maximum)));

        assertBothInvalidField(withTextField(field, stringOfLength(aboveMaximum)), field);
    }

    @Test
    void shouldAcceptHealthcareContactEmailAtMaximumLength() {
        assertBothValid(withEmail(emailOfLength(253)));
        assertBothValid(withEmail(emailOfLength(254)));
    }

    @Test
    void shouldRejectHealthcareContactEmailAboveMaximumLength() {
        assertBothInvalidField(withEmail(emailOfLength(255)), "email");
    }

    @Test
    void shouldRejectMalformedHealthcareContactEmail() {
        assertBothInvalidField(withEmail("not-an-email"), "email");
    }

    @Test
    void shouldAcceptNullOptionalHealthcareContactFields() {
        HealthcareContactRequests requests = requests("Example Clinician", null, null, null, null, null, null);

        assertBothValid(requests);
    }

    @Test
    void shouldApplyNestedHealthcareAddressValidation() {
        AddressRequest invalidAddress = new AddressRequest("", null, "Exampletown", null, "AB1 2CD", "United Kingdom");

        HealthcareContactRequests requests = requests(
                "Example Clinician",
                null,
                null,
                null,
                null,
                invalidAddress,
                null);

        assertBothInvalidField(requests, "address.addressLine1");
    }

    private static Stream<String> invalidRequiredNames() {
        return Stream.of("", "   ", null);
    }

    private static Stream<Arguments> optionalTextBoundaries() {
        return Stream.of(
                Arguments.of("role", 149, 150, 151),
                Arguments.of("organisation", 199, 200, 201),
                Arguments.of("phoneNumber", 49, 50, 51),
                Arguments.of("notes", 1999, 2000, 2001));
    }

    private static HealthcareContactRequests withName(String name) {
        return requests(name, null, null, null, null, null, null);
    }

    private static HealthcareContactRequests withEmail(String email) {
        return requests("Example Clinician", null, null, null, email, null, null);
    }

    private static HealthcareContactRequests withTextField(String field, String value) {
        return switch (field) {
            case "role" -> requests("Example Clinician", value, null, null, null, null, null);
            case "organisation" -> requests("Example Clinician", null, value, null, null, null, null);
            case "phoneNumber" -> requests("Example Clinician", null, null, value, null, null, null);
            case "notes" -> requests("Example Clinician", null, null, null, null, null, value);
            default -> throw new IllegalArgumentException("Unsupported healthcare contact field: " + field);
        };
    }

    private static HealthcareContactRequests requests(
            String name,
            String role,
            String organisation,
            String phoneNumber,
            String email,
            AddressRequest address,
            String notes) {
        return new HealthcareContactRequests(
                new CreateHealthcareContactRequest(name, role, organisation, phoneNumber, email, address, notes),
                new UpdateHealthcareContactRequest(name, role, organisation, phoneNumber, email, address, notes));
    }

    private static void assertBothValid(
            HealthcareContactRequests requests) {
        assertValid(requests.createRequest());
        assertValid(requests.updateRequest());
    }

    private static void assertBothInvalidField(HealthcareContactRequests requests, String field) {
        assertInvalidField(requests.createRequest(), field);
        assertInvalidField(requests.updateRequest(), field);
    }

    private static String emailOfLength(int length) {
        String prefix = "a@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(63) + ".";

        int remainingCharacters = length - prefix.length();

        if (remainingCharacters < 1 || remainingCharacters > 63) {
            throw new IllegalArgumentException("Unsupported synthetic email length: " + length);
        }

        return prefix + "e".repeat(remainingCharacters);
    }

    private record HealthcareContactRequests(CreateHealthcareContactRequest createRequest,
                                             UpdateHealthcareContactRequest updateRequest) {
    }
}