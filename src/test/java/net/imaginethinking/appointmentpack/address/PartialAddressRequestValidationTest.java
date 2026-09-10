package net.imaginethinking.appointmentpack.address;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.*;

/**
 * Checks the validation rules used for partial address request.
 */
class PartialAddressRequestValidationTest {

    @Test
    void shouldAcceptRepresentativePartialAddress() {
        PartialAddressRequest request = new PartialAddressRequest(
                "Example Hospital",
                null,
                "Exampletown",
                null,
                "AB1 2CD",
                null
        );

        assertValid(request);
    }

    @Test
    void shouldAcceptAllNullFields() {
        PartialAddressRequest request = new PartialAddressRequest(
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertValid(request);
    }

    @Test
    void shouldAcceptBlankFieldsBecausePartialAddressValuesAreOptional() {
        PartialAddressRequest request = new PartialAddressRequest(
                "",
                "   ",
                "",
                "   ",
                "",
                "   "
        );

        assertValid(request);
    }

    @ParameterizedTest(name = "{0}: {1} and {2} valid, {3} invalid")
    @MethodSource("fieldLengthBoundaries")
    void shouldEnforcePartialAddressFieldLengthBoundaries(
            String field,
            int belowMaximum,
            int maximum,
            int aboveMaximum
    ) {
        assertValid(withField(field, stringOfLength(belowMaximum)));
        assertValid(withField(field, stringOfLength(maximum)));
        assertInvalidField(withField(field, stringOfLength(aboveMaximum)), field);
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
     * Returns the request variants with the selected field value replaced for the validation test.
     */
    private static PartialAddressRequest withField(String field, String value) {
        return switch (field) {
            case "addressLine1" -> new PartialAddressRequest(value, null, null, null, null, null);
            case "addressLine2" -> new PartialAddressRequest(null, value, null, null, null, null);
            case "townCity" -> new PartialAddressRequest(null, null, value, null, null, null);
            case "county" -> new PartialAddressRequest(null, null, null, value, null, null);
            case "postcode" -> new PartialAddressRequest(null, null, null, null, value, null);
            case "country" -> new PartialAddressRequest(null, null, null, null, null, value);
            default -> throw new IllegalArgumentException("Unsupported address field: " + field);
        };
    }
}