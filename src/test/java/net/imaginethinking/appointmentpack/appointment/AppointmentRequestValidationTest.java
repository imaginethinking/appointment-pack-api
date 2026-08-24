package net.imaginethinking.appointmentpack.appointment;

import net.imaginethinking.appointmentpack.address.PartialAddressRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.Stream;

import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.*;

class AppointmentRequestValidationTest {

    @Test
    void shouldAcceptRepresentativeAppointmentRequests() {
        AppointmentRequests requests = requests(
                LocalDate.of(2026, 9, 1),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30),
                "Example Service",
                "Follow-up",
                "Clinical Team",
                "Example Clinic",
                new PartialAddressRequest("1 Example Road", null, "Exampletown", null, "AB1 2CD", "United Kingdom"),
                "Bring current medication list");

        assertAllValid(requests);
    }

    @Test
    void shouldRejectNullAppointmentDate() {
        AppointmentRequests requests = requests(null, LocalTime.of(10, 0), null, null, null, null, null, null, null);

        assertAllInvalidField(requests, "date");
    }

    @Test
    void shouldRejectNullAppointmentStartTime() {
        AppointmentRequests requests = requests(
                LocalDate.of(2026, 9, 1),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertAllInvalidField(requests, "startTime");
    }

    @Test
    void shouldAcceptNullOptionalAppointmentFields() {
        AppointmentRequests requests = requests(
                LocalDate.of(2026, 9, 1),
                LocalTime.of(10, 0),
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertAllValid(requests);
    }

    @ParameterizedTest(name = "{0}: {1} and {2} valid, {3} invalid")
    @MethodSource("textFieldBoundaries")
    void shouldEnforceAppointmentTextFieldBoundaries(String field, int belowMaximum, int maximum, int aboveMaximum) {
        assertAllValid(withTextField(field, stringOfLength(belowMaximum)));
        assertAllValid(withTextField(field, stringOfLength(maximum)));

        assertAllInvalidField(withTextField(field, stringOfLength(aboveMaximum)), field);
    }

    @Test
    void shouldApplyNestedPartialAddressValidation() {
        PartialAddressRequest invalidAddress = new PartialAddressRequest(
                stringOfLength(151),
                null,
                null,
                null,
                null,
                null);

        AppointmentRequests requests = requests(
                LocalDate.of(2026, 9, 1),
                LocalTime.of(10, 0),
                null,
                null,
                null,
                null,
                null,
                invalidAddress,
                null);

        assertAllInvalidField(requests, "address.addressLine1");
    }

    private static Stream<Arguments> textFieldBoundaries() {
        return Stream.of(
                Arguments.of("service", 249, 250, 251),
                Arguments.of("appointmentType", 249, 250, 251),
                Arguments.of("clinicianOrTeam", 249, 250, 251),
                Arguments.of("locationName", 249, 250, 251),
                Arguments.of("notes", 1999, 2000, 2001));
    }

    private static AppointmentRequests withTextField(String field, String value) {
        return switch (field) {
            case "service" ->
                    requests(LocalDate.of(2026, 9, 1), LocalTime.of(10, 0), null, value, null, null, null, null, null);
            case "appointmentType" ->
                    requests(LocalDate.of(2026, 9, 1), LocalTime.of(10, 0), null, null, value, null, null, null, null);
            case "clinicianOrTeam" ->
                    requests(LocalDate.of(2026, 9, 1), LocalTime.of(10, 0), null, null, null, value, null, null, null);
            case "locationName" ->
                    requests(LocalDate.of(2026, 9, 1), LocalTime.of(10, 0), null, null, null, null, value, null, null);
            case "notes" ->
                    requests(LocalDate.of(2026, 9, 1), LocalTime.of(10, 0), null, null, null, null, null, null, value);
            default -> throw new IllegalArgumentException("Unsupported appointment field: " + field);
        };
    }

    private static AppointmentRequests requests(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String service,
            String appointmentType,
            String clinicianOrTeam,
            String locationName,
            PartialAddressRequest address,
            String notes) {
        return new AppointmentRequests(
                new CreateAppointmentRequest(
                        date,
                        startTime,
                        endTime,
                        service,
                        appointmentType,
                        clinicianOrTeam,
                        locationName,
                        address,
                        notes),
                new UpdateAppointmentRequest(
                        date,
                        startTime,
                        endTime,
                        service,
                        appointmentType,
                        clinicianOrTeam,
                        locationName,
                        address,
                        notes),
                new AppointmentConfirmationRequest(
                        date,
                        startTime,
                        endTime,
                        service,
                        appointmentType,
                        clinicianOrTeam,
                        locationName,
                        address,
                        notes));
    }

    private static void assertAllValid(AppointmentRequests requests) {
        assertValid(requests.createRequest());
        assertValid(requests.updateRequest());
        assertValid(requests.confirmationRequest());
    }

    private static void assertAllInvalidField(AppointmentRequests requests, String field) {
        assertInvalidField(requests.createRequest(), field);
        assertInvalidField(requests.updateRequest(), field);
        assertInvalidField(requests.confirmationRequest(), field);
    }

    private record AppointmentRequests(
            CreateAppointmentRequest createRequest,
            UpdateAppointmentRequest updateRequest,
            AppointmentConfirmationRequest confirmationRequest
    ) {
    }
}