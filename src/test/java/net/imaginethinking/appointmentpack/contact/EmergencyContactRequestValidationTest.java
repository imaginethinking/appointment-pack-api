package net.imaginethinking.appointmentpack.contact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.*;

class EmergencyContactRequestValidationTest {

    @Test
    void shouldAcceptRepresentativeEmergencyContactRequests() {
        EmergencyContactRequests requests = requests(
                "Example Contact",
                "Relative",
                "07000 000000",
                "07000 000001",
                "contact@example.com",
                "Emergency contact");

        assertBothValid(requests);
    }

    @ParameterizedTest(name = "{0}: {1} and {2} valid, {3} invalid")
    @MethodSource("requiredFieldBoundaries")
    void shouldEnforceRequiredEmergencyContactBoundaries(
            String field,
            int belowMaximum,
            int maximum,
            int aboveMaximum) {
        assertBothValid(withRequiredField(field, stringOfLength(belowMaximum)));

        assertBothValid(withRequiredField(field, stringOfLength(maximum)));

        assertBothInvalidField(withRequiredField(field, stringOfLength(aboveMaximum)), field);
    }

    @ParameterizedTest
    @MethodSource("invalidRequiredFieldValues")
    void shouldRejectBlankOrNullRequiredEmergencyContactFields(String field, String value) {
        assertBothInvalidField(withRequiredField(field, value), field);
    }

    @ParameterizedTest(name = "{0}: {1} and {2} valid, {3} invalid")
    @MethodSource("optionalTextBoundaries")
    void shouldEnforceOptionalEmergencyContactBoundaries(
            String field,
            int belowMaximum,
            int maximum,
            int aboveMaximum) {
        assertBothValid(withOptionalField(field, stringOfLength(belowMaximum)));

        assertBothValid(withOptionalField(field, stringOfLength(maximum)));

        assertBothInvalidField(withOptionalField(field, stringOfLength(aboveMaximum)), field);
    }

    @Test
    void shouldAcceptEmergencyContactEmailAtMaximumLength() {
        assertBothValid(withEmail(emailOfLength(253)));
        assertBothValid(withEmail(emailOfLength(254)));
    }

    @Test
    void shouldRejectEmergencyContactEmailAboveMaximumLength() {
        assertBothInvalidField(withEmail(emailOfLength(255)), "email");
    }

    @Test
    void shouldRejectMalformedEmergencyContactEmail() {
        assertBothInvalidField(withEmail("not-an-email"), "email");
    }

    @Test
    void shouldAcceptNullOptionalEmergencyContactFields() {
        EmergencyContactRequests requests = requests("Example Contact", "Relative", "07000 000000", null, null, null);

        assertBothValid(requests);
    }

    private static Stream<Arguments> requiredFieldBoundaries() {
        return Stream.of(
                Arguments.of("name", 199, 200, 201),
                Arguments.of("relationship", 149, 150, 151),
                Arguments.of("phoneNumber", 49, 50, 51));
    }

    private static Stream<Arguments> invalidRequiredFieldValues() {
        return Stream.of(
                Arguments.of("name", ""),
                Arguments.of("name", "   "),
                Arguments.of("name", null),
                Arguments.of("relationship", ""),
                Arguments.of("relationship", "   "),
                Arguments.of("relationship", null),
                Arguments.of("phoneNumber", ""),
                Arguments.of("phoneNumber", "   "),
                Arguments.of("phoneNumber", null));
    }

    private static Stream<Arguments> optionalTextBoundaries() {
        return Stream.of(Arguments.of("alternativePhoneNumber", 49, 50, 51), Arguments.of("notes", 1999, 2000, 2001));
    }

    private static EmergencyContactRequests withRequiredField(String field, String value) {
        return switch (field) {
            case "name" -> requests(value, "Relative", "07000 000000", null, null, null);
            case "relationship" -> requests("Example Contact", value, "07000 000000", null, null, null);
            case "phoneNumber" -> requests("Example Contact", "Relative", value, null, null, null);
            default -> throw new IllegalArgumentException("Unsupported emergency contact field: " + field);
        };
    }

    private static EmergencyContactRequests withOptionalField(String field, String value) {
        return switch (field) {
            case "alternativePhoneNumber" -> requests("Example Contact", "Relative", "07000 000000", value, null, null);
            case "notes" -> requests("Example Contact", "Relative", "07000 000000", null, null, value);
            default -> throw new IllegalArgumentException("Unsupported emergency contact field: " + field);
        };
    }

    private static EmergencyContactRequests withEmail(String email) {
        return requests("Example Contact", "Relative", "07000 000000", null, email, null);
    }

    private static EmergencyContactRequests requests(
            String name,
            String relationship,
            String phoneNumber,
            String alternativePhoneNumber,
            String email,
            String notes) {
        return new EmergencyContactRequests(
                new CreateEmergencyContactRequest(
                        name,
                        relationship,
                        phoneNumber,
                        alternativePhoneNumber,
                        email,
                        notes),
                new UpdateEmergencyContactRequest(
                        name,
                        relationship,
                        phoneNumber,
                        alternativePhoneNumber,
                        email,
                        notes));
    }

    private static void assertBothValid(
            EmergencyContactRequests requests) {
        assertValid(requests.createRequest());
        assertValid(requests.updateRequest());
    }

    private static void assertBothInvalidField(EmergencyContactRequests requests, String field) {
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

    private record EmergencyContactRequests(CreateEmergencyContactRequest createRequest,
                                            UpdateEmergencyContactRequest updateRequest) {
    }
}