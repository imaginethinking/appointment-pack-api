package net.imaginethinking.appointmentpack.profile;

import net.imaginethinking.appointmentpack.address.AddressRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.assertInvalidField;
import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.assertValid;
import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.stringOfLength;

class UpdateProfileRequestValidationTest {

    @Test
    void shouldAcceptRepresentativeProfileRequest() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Patient",
                "User",
                LocalDate.of(1990, 1, 1),
                "Female",
                validAddress()
        );

        assertValid(request);
    }

    @ParameterizedTest(name = "{0}: {1} and {2} valid, {3} invalid")
    @MethodSource("nameBoundaries")
    void shouldEnforceProfileNameBoundaries(
            String field,
            int belowMaximum,
            int maximum,
            int aboveMaximum
    ) {
        assertValid(withName(field, stringOfLength(belowMaximum)));
        assertValid(withName(field, stringOfLength(maximum)));
        assertInvalidField(withName(field, stringOfLength(aboveMaximum)), field);
    }

    @ParameterizedTest
    @MethodSource("requiredNameValues")
    void shouldRejectBlankOrNullProfileNames(String field, String value) {
        assertInvalidField(withName(field, value), field);
    }

    @Test
    void shouldAcceptPastDateOfBirth() {
        assertValid(withDateOfBirth(LocalDate.now().minusDays(1)));
    }

    @Test
    void shouldRejectPresentDateOfBirth() {
        assertInvalidField(withDateOfBirth(LocalDate.now()), "dateOfBirth");
    }

    @Test
    void shouldRejectFutureDateOfBirth() {
        assertInvalidField(withDateOfBirth(LocalDate.now().plusDays(1)), "dateOfBirth");
    }

    @Test
    void shouldRejectNullDateOfBirth() {
        assertInvalidField(withDateOfBirth(null), "dateOfBirth");
    }

    @Test
    void shouldEnforceGenderLengthBoundary() {
        assertValid(withGender(stringOfLength(49)));
        assertValid(withGender(stringOfLength(50)));
        assertInvalidField(withGender(stringOfLength(51)), "gender");
    }

    @Test
    void shouldAcceptNullAndBlankGender() {
        assertValid(withGender(null));
        assertValid(withGender(""));
        assertValid(withGender("   "));
    }

    @Test
    void shouldAcceptNullAddress() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Patient",
                "User",
                LocalDate.of(1990, 1, 1),
                null,
                null
        );

        assertValid(request);
    }

    @Test
    void shouldApplyNestedAddressValidation() {
        AddressRequest invalidAddress = new AddressRequest(
                "",
                null,
                "Exampletown",
                null,
                "AB1 2CD",
                "United Kingdom"
        );

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Patient",
                "User",
                LocalDate.of(1990, 1, 1),
                null,
                invalidAddress
        );

        assertInvalidField(request, "address.addressLine1");
    }

    private static Stream<Arguments> nameBoundaries() {
        return Stream.of(
                Arguments.of("firstName", 99, 100, 101),
                Arguments.of("lastName", 99, 100, 101)
        );
    }

    private static Stream<Arguments> requiredNameValues() {
        return Stream.of(
                Arguments.of("firstName", ""),
                Arguments.of("firstName", "   "),
                Arguments.of("firstName", null),
                Arguments.of("lastName", ""),
                Arguments.of("lastName", "   "),
                Arguments.of("lastName", null)
        );
    }

    private static UpdateProfileRequest withName(String field, String value) {
        return switch (field) {
            case "firstName" -> new UpdateProfileRequest(
                    value,
                    "User",
                    LocalDate.of(1990, 1, 1),
                    null,
                    null
            );
            case "lastName" -> new UpdateProfileRequest(
                    "Patient",
                    value,
                    LocalDate.of(1990, 1, 1),
                    null,
                    null
            );
            default -> throw new IllegalArgumentException(
                    "Unsupported profile name field: " + field
            );
        };
    }

    private static UpdateProfileRequest withDateOfBirth(LocalDate dateOfBirth) {
        return new UpdateProfileRequest(
                "Patient",
                "User",
                dateOfBirth,
                null,
                null
        );
    }

    private static UpdateProfileRequest withGender(String gender) {
        return new UpdateProfileRequest(
                "Patient",
                "User",
                LocalDate.of(1990, 1, 1),
                gender,
                null
        );
    }

    private static AddressRequest validAddress() {
        return new AddressRequest(
                "1 Example Street",
                null,
                "Exampletown",
                null,
                "AB1 2CD",
                "United Kingdom"
        );
    }
}