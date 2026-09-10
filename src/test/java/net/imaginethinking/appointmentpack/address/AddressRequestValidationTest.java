package net.imaginethinking.appointmentpack.address;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.assertInvalidField;
import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.assertValid;
import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.stringOfLength;

/**
 * Checks the validation rules used for address request.
 */
class AddressRequestValidationTest {

    @Test
    void shouldAcceptRepresentativeCompleteAddress() {
        AddressRequest request = new AddressRequest(
                "1 Example Street",
                "Example Building",
                "Exampletown",
                "Exampleshire",
                "AB1 2CD",
                "United Kingdom"
        );

        assertValid(request);
    }

    @ParameterizedTest(name = "{0}: {1} and {2} valid, {3} invalid")
    @MethodSource("fieldLengthBoundaries")
    void shouldEnforceAddressFieldLengthBoundaries(
            String field,
            int belowMaximum,
            int maximum,
            int aboveMaximum
    ) {
        assertValid(withField(field, stringOfLength(belowMaximum)));
        assertValid(withField(field, stringOfLength(maximum)));
        assertInvalidField(withField(field, stringOfLength(aboveMaximum)), field);
    }

    @ParameterizedTest(name = "{0} should reject required value {1}")
    @MethodSource("blankRequiredFields")
    void shouldRejectBlankRequiredAddressFields(String field, String value) {
        assertInvalidField(withField(field, value), field);
    }

    @ParameterizedTest(name = "{0} should reject null")
    @MethodSource("requiredFields")
    void shouldRejectNullRequiredAddressFields(String field) {
        assertInvalidField(withField(field, null), field);
    }

    @Test
    void shouldAcceptNullOptionalAddressFields() {
        AddressRequest request = new AddressRequest(
                "1 Example Street",
                null,
                "Exampletown",
                null,
                "AB1 2CD",
                "United Kingdom"
        );

        assertValid(request);
    }

    /**
     * Provides field length values used by the parameterised tests.
     */
    private static Stream<Arguments> fieldLengthBoundaries() {
        return Stream.of(
                Arguments.of("addressLine1", 149, 150, 151),
                Arguments.of("addressLine2", 149, 150, 151),
                Arguments.of("townCity", 99, 100, 101),
                Arguments.of("county", 99, 100, 101),
                Arguments.of("postcode", 19, 20, 21),
                Arguments.of("country", 99, 100, 101)
        );
    }

    /**
     * Returns the blank required fields used by the surrounding tests.
     */
    private static Stream<Arguments> blankRequiredFields() {
        return Stream.of(
                Arguments.of("addressLine1", ""),
                Arguments.of("addressLine1", "   "),
                Arguments.of("townCity", ""),
                Arguments.of("townCity", "   "),
                Arguments.of("postcode", ""),
                Arguments.of("postcode", "   "),
                Arguments.of("country", ""),
                Arguments.of("country", "   ")
        );
    }

    /**
     * Provides the required fields values used by the parameterised validation tests.
     */
    private static Stream<Arguments> requiredFields() {
        return Stream.of(
                Arguments.of("addressLine1"),
                Arguments.of("townCity"),
                Arguments.of("postcode"),
                Arguments.of("country")
        );
    }

    /**
     * Returns the request variants with the selected field value replaced for the validation test.
     */
    private static AddressRequest withField(String field, String value) {
        return switch (field) {
            case "addressLine1" -> new AddressRequest(
                    value,
                    "Example Building",
                    "Exampletown",
                    "Exampleshire",
                    "AB1 2CD",
                    "United Kingdom"
            );
            case "addressLine2" -> new AddressRequest(
                    "1 Example Street",
                    value,
                    "Exampletown",
                    "Exampleshire",
                    "AB1 2CD",
                    "United Kingdom"
            );
            case "townCity" -> new AddressRequest(
                    "1 Example Street",
                    "Example Building",
                    value,
                    "Exampleshire",
                    "AB1 2CD",
                    "United Kingdom"
            );
            case "county" -> new AddressRequest(
                    "1 Example Street",
                    "Example Building",
                    "Exampletown",
                    value,
                    "AB1 2CD",
                    "United Kingdom"
            );
            case "postcode" -> new AddressRequest(
                    "1 Example Street",
                    "Example Building",
                    "Exampletown",
                    "Exampleshire",
                    value,
                    "United Kingdom"
            );
            case "country" -> new AddressRequest(
                    "1 Example Street",
                    "Example Building",
                    "Exampletown",
                    "Exampleshire",
                    "AB1 2CD",
                    value
            );
            default -> throw new IllegalArgumentException("Unsupported address field: " + field);
        };
    }
}