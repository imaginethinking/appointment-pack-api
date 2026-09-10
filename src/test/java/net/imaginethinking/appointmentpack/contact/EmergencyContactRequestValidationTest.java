package net.imaginethinking.appointmentpack.contact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.*;

/**
 * Checks the validation rules used for emergency contact request.
 */
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

    /**
     * Provides the required field boundaries values used by the parameterised validation tests.
     */
    private static Stream<Arguments> requiredFieldBoundaries() {
        return Stream.of(
                Arguments.of("name", 199, 200, 201),
                Arguments.of("relationship", 149, 150, 151),
                Arguments.of("phoneNumber", 49, 50, 51));
    }

    /**
     * Provides invalid required field values values used by the parameterised validation tests.
     */
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

    /**
     * Provides boundary values for the optional text boundaries fields.
     */
    private static Stream<Arguments> optionalTextBoundaries() {
        return Stream.of(Arguments.of("alternativePhoneNumber", 49, 50, 51), Arguments.of("notes", 1999, 2000, 2001));
    }

    /**
     * Returns the request variants with the selected required field value replaced for the validation test.
     */
    private static EmergencyContactRequests withRequiredField(String field, String value) {
        return switch (field) {
            case "name" -> requests(value, "Relative", "07000 000000", null, null, null);
            case "relationship" -> requests("Example Contact", value, "07000 000000", null, null, null);
            case "phoneNumber" -> requests("Example Contact", "Relative", value, null, null, null);
            default -> throw new IllegalArgumentException("Unsupported emergency contact field: " + field);
        };
    }

    /**
     * Returns the request variants with the selected optional field value replaced for the validation test.
     */
    private static EmergencyContactRequests withOptionalField(String field, String value) {
        return switch (field) {
            case "alternativePhoneNumber" -> requests("Example Contact", "Relative", "07000 000000", value, null, null);
            case "notes" -> requests("Example Contact", "Relative", "07000 000000", null, null, value);
            default -> throw new IllegalArgumentException("Unsupported emergency contact field: " + field);
        };
    }

    /**
     * Returns a test request with the email value replaced by the supplied value.
     */
    private static EmergencyContactRequests withEmail(String email) {
        return requests("Example Contact", "Relative", "07000 000000", null, email, null);
    }

    /**
     * Creates the create and update request variants using the supplied values.
     */
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

    /**
     * Checks that both request variants pass validation.
     */
    private static void assertBothValid(
            EmergencyContactRequests requests) {
        assertValid(requests.createRequest());
        assertValid(requests.updateRequest());
    }

    /**
     * Checks that both request variants report a validation error for the given field.
     */
    private static void assertBothInvalidField(EmergencyContactRequests requests, String field) {
        assertInvalidField(requests.createRequest(), field);
        assertInvalidField(requests.updateRequest(), field);
    }

    /**
     * Creates an email address with the requested length for boundary validation tests.
     */
    private static String emailOfLength(int length) {
        String prefix = "a@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(63) + ".";

        int remainingCharacters = length - prefix.length();

        if (remainingCharacters < 1 || remainingCharacters > 63) {
            throw new IllegalArgumentException("Unsupported synthetic email length: " + length);
        }

        return prefix + "e".repeat(remainingCharacters);
    }

    /**
     * Checks the emergency contact requests behaviour covered by this test class.
     */
    private record EmergencyContactRequests(CreateEmergencyContactRequest createRequest,
                                            UpdateEmergencyContactRequest updateRequest) {
    }
}