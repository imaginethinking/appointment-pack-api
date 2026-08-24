package net.imaginethinking.appointmentpack.patientrecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.*;

class PatientRecordRequestValidationTest {

    @Test
    void shouldAcceptRepresentativePatientRecordRequests() {
        RequestPair requests = requests(
                "1234567890",
                "1234567890",
                "1234567890",
                new BigDecimal("1.80"),
                HeightUnit.METERS,
                new BigDecimal("75.00"),
                WeightUnit.KILOGRAMS,
                BloodType.O_POSITIVE);

        assertBothValid(requests);
    }

    @ParameterizedTest(name = "{0}: {1} and {2} valid, {3} invalid")
    @MethodSource("identifierBoundaries")
    void shouldEnforceHealthcareIdentifierLengthBoundaries(
            String field,
            int belowMaximum,
            int maximum,
            int aboveMaximum) {
        assertBothValid(withIdentifier(field, stringOfLength(belowMaximum)));
        assertBothValid(withIdentifier(field, stringOfLength(maximum)));
        assertBothInvalidField(withIdentifier(field, stringOfLength(aboveMaximum)), field);
    }

    @Test
    void shouldAcceptNullOptionalHealthcareIdentifiers() {
        RequestPair requests = requests(null, null, null, null, null, null, null, null);

        assertBothValid(requests);
    }

    @Test
    void shouldAcceptBlankHealthcareIdentifiersAtValidationLayer() {
        RequestPair requests = requests("", "   ", "", null, null, null, null, null);

        assertBothValid(requests);
    }

    @Test
    void shouldAcceptMeasurementAtMinimumValue() {
        assertBothValid(withHeight(new BigDecimal("0.01")));
        assertBothValid(withWeight(new BigDecimal("0.01")));
    }

    @Test
    void shouldRejectZeroMeasurementValue() {
        assertBothInvalidField(withHeight(new BigDecimal("0.00")), "height");
        assertBothInvalidField(withWeight(new BigDecimal("0.00")), "weight");
    }

    @Test
    void shouldRejectNegativeMeasurementValue() {
        assertBothInvalidField(withHeight(new BigDecimal("-0.01")), "height");
        assertBothInvalidField(withWeight(new BigDecimal("-0.01")), "weight");
    }

    @Test
    void shouldAcceptMeasurementAtMaximumDigitBoundary() {
        assertBothValid(withHeight(new BigDecimal("9999.99")));
        assertBothValid(withWeight(new BigDecimal("9999.99")));
    }

    @Test
    void shouldRejectMeasurementAboveMaximumIntegerDigits() {
        assertBothInvalidField(withHeight(new BigDecimal("10000.00")), "height");
        assertBothInvalidField(withWeight(new BigDecimal("10000.00")), "weight");
    }

    @Test
    void shouldRejectMeasurementAboveMaximumFractionDigits() {
        assertBothInvalidField(withHeight(new BigDecimal("1.234")), "height");
        assertBothInvalidField(withWeight(new BigDecimal("1.234")), "weight");
    }

    @Test
    void shouldAcceptNullMeasurementValueAndUnitAtValidationLayer() {
        RequestPair requests = requests(null, null, null, null, null, null, null, null);

        assertBothValid(requests);
    }

    @Test
    void shouldLeaveMeasurementValueAndUnitPairingToServiceLayer() {
        RequestPair heightValueWithoutUnit = requests(null, null, null, new BigDecimal("1.80"), null, null, null, null);

        RequestPair heightUnitWithoutValue = requests(null, null, null, null, HeightUnit.METERS, null, null, null);

        assertBothValid(heightValueWithoutUnit);
        assertBothValid(heightUnitWithoutValue);
    }

    private static Stream<Arguments> identifierBoundaries() {
        return Stream.of(
                Arguments.of("nhsNumber", 9, 10, 11),
                Arguments.of("chiNumber", 9, 10, 11),
                Arguments.of("hcNumber", 9, 10, 11));
    }

    private static RequestPair withIdentifier(String field, String value) {
        return switch (field) {
            case "nhsNumber" -> requests(value, null, null, null, null, null, null, null);
            case "chiNumber" -> requests(null, value, null, null, null, null, null, null);
            case "hcNumber" -> requests(null, null, value, null, null, null, null, null);
            default -> throw new IllegalArgumentException("Unsupported patient identifier field: " + field);
        };
    }

    private static RequestPair withHeight(BigDecimal height) {
        return requests(null, null, null, height, HeightUnit.METERS, null, null, null);
    }

    private static RequestPair withWeight(BigDecimal weight) {
        return requests(null, null, null, null, null, weight, WeightUnit.KILOGRAMS, null);
    }

    private static RequestPair requests(
            String nhsNumber,
            String chiNumber,
            String hcNumber,
            BigDecimal height,
            HeightUnit heightUnit,
            BigDecimal weight,
            WeightUnit weightUnit,
            BloodType bloodType) {
        return new RequestPair(
                new CreatePatientRecordRequest(
                        nhsNumber,
                        chiNumber,
                        hcNumber,
                        height,
                        heightUnit,
                        weight,
                        weightUnit,
                        bloodType),
                new UpdatePatientRecordRequest(
                        nhsNumber,
                        chiNumber,
                        hcNumber,
                        height,
                        heightUnit,
                        weight,
                        weightUnit,
                        bloodType));
    }

    private static void assertBothValid(RequestPair requests) {
        assertValid(requests.createRequest());
        assertValid(requests.updateRequest());
    }

    private static void assertBothInvalidField(RequestPair requests, String field) {
        assertInvalidField(requests.createRequest(), field);
        assertInvalidField(requests.updateRequest(), field);
    }

    private record RequestPair(CreatePatientRecordRequest createRequest, UpdatePatientRecordRequest updateRequest) {
    }
}