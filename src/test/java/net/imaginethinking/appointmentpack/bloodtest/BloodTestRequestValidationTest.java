package net.imaginethinking.appointmentpack.bloodtest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.*;

class BloodTestRequestValidationTest {

    @Test
    void shouldAcceptRepresentativeBloodTestRequests() {
        BloodTestRequests requests = requests(
                "Example Blood Test",
                LocalDate.now(),
                "Example Laboratory",
                "Routine test",
                List.of(validResult()));

        assertBothValid(requests);
    }

    @ParameterizedTest(name = "{0}: {1} and {2} valid, {3} invalid")
    @MethodSource("parentTextBoundaries")
    void shouldEnforceBloodTestTextBoundaries(String field, int belowMaximum, int maximum, int aboveMaximum) {
        assertBothValid(withParentTextField(field, stringOfLength(belowMaximum)));

        assertBothValid(withParentTextField(field, stringOfLength(maximum)));

        assertBothInvalidField(withParentTextField(field, stringOfLength(aboveMaximum)), field);
    }

    @Test
    void shouldAcceptPastBloodTestDate() {
        assertBothValid(withDate(LocalDate.now().minusDays(1)));
    }

    @Test
    void shouldAcceptPresentBloodTestDate() {
        assertBothValid(withDate(LocalDate.now()));
    }

    @Test
    void shouldRejectFutureBloodTestDate() {
        assertBothInvalidField(withDate(LocalDate.now().plusDays(1)), "testDate");
    }

    @Test
    void shouldRejectNullBloodTestDate() {
        assertBothInvalidField(withDate(null), "testDate");
    }

    @Test
    void shouldAcceptBloodTestWithOneResult() {
        assertBothValid(withResults(resultsOfSize(1)));
    }

    @Test
    void shouldAcceptBloodTestImmediatelyBelowResultLimit() {
        assertBothValid(withResults(resultsOfSize(99)));
    }

    @Test
    void shouldAcceptBloodTestAtResultLimit() {
        assertBothValid(withResults(resultsOfSize(100)));
    }

    @Test
    void shouldRejectBloodTestAboveResultLimit() {
        assertBothInvalidField(withResults(resultsOfSize(101)), "results");
    }

    @Test
    void shouldRejectEmptyBloodTestResults() {
        assertBothInvalidField(withResults(List.of()), "results");
    }

    @Test
    void shouldRejectNullBloodTestResults() {
        assertBothInvalidField(withResults(null), "results");
    }

    @Test
    void shouldApplyNestedBloodResultValidation() {
        BloodTestResultRequest invalidResult = new BloodTestResultRequest("", "1", null, null, null);

        BloodTestRequests requests = withResults(List.of(invalidResult));

        assertInvalid(requests.createRequest());
        assertInvalid(requests.updateRequest());
    }

    @Test
    void shouldAcceptRepresentativeBloodTestResult() {
        assertValid(validResult());
    }

    @ParameterizedTest(name = "{0}: {1} and {2} valid, {3} invalid")
    @MethodSource("resultTextBoundaries")
    void shouldEnforceBloodTestResultTextBoundaries(String field, int belowMaximum, int maximum, int aboveMaximum) {
        assertValid(withResultTextField(field, stringOfLength(belowMaximum)));

        assertValid(withResultTextField(field, stringOfLength(maximum)));

        assertInvalidField(withResultTextField(field, stringOfLength(aboveMaximum)), field);
    }

    @ParameterizedTest
    @MethodSource("invalidRequiredResultValues")
    void shouldRejectBlankOrNullRequiredBloodResultFields(String field, String value) {
        assertInvalidField(withResultTextField(field, value), field);
    }

    @Test
    void shouldAcceptNullOptionalBloodResultFields() {
        BloodTestResultRequest result = new BloodTestResultRequest("Example Analyte", "1", null, null, null);

        assertValid(result);
    }

    private static Stream<Arguments> parentTextBoundaries() {
        return Stream.of(
                Arguments.of("title", 199, 200, 201),
                Arguments.of("provider", 199, 200, 201),
                Arguments.of("notes", 1999, 2000, 2001));
    }

    private static Stream<Arguments> resultTextBoundaries() {
        return Stream.of(
                Arguments.of("analyteName", 199, 200, 201),
                Arguments.of("resultValue", 99, 100, 101),
                Arguments.of("unit", 99, 100, 101),
                Arguments.of("referenceRange", 149, 150, 151));
    }

    private static Stream<Arguments> invalidRequiredResultValues() {
        return Stream.of(
                Arguments.of("analyteName", ""),
                Arguments.of("analyteName", "   "),
                Arguments.of("analyteName", null),
                Arguments.of("resultValue", ""),
                Arguments.of("resultValue", "   "),
                Arguments.of("resultValue", null));
    }

    private static BloodTestRequests withDate(LocalDate date) {
        return requests(null, date, null, null, List.of(validResult()));
    }

    private static BloodTestRequests withResults(
            List<BloodTestResultRequest> results) {
        return requests(null, LocalDate.now(), null, null, results);
    }

    private static BloodTestRequests withParentTextField(String field, String value) {
        return switch (field) {
            case "title" -> requests(value, LocalDate.now(), null, null, List.of(validResult()));
            case "provider" -> requests(null, LocalDate.now(), value, null, List.of(validResult()));
            case "notes" -> requests(null, LocalDate.now(), null, value, List.of(validResult()));
            default -> throw new IllegalArgumentException("Unsupported blood test field: " + field);
        };
    }

    private static BloodTestResultRequest withResultTextField(String field, String value) {
        return switch (field) {
            case "analyteName" -> new BloodTestResultRequest(value, "1", null, null, null);
            case "resultValue" -> new BloodTestResultRequest("Example Analyte", value, null, null, null);
            case "unit" -> new BloodTestResultRequest("Example Analyte", "1", value, null, null);
            case "referenceRange" -> new BloodTestResultRequest("Example Analyte", "1", null, value, null);
            default -> throw new IllegalArgumentException("Unsupported blood result field: " + field);
        };
    }

    private static List<BloodTestResultRequest> resultsOfSize(
            int size) {
        return IntStream.range(0, size)
                .mapToObj(index -> new BloodTestResultRequest(
                        "Analyte " + index,
                        Integer.toString(index),
                        null,
                        null,
                        BloodTestResultFlag.NORMAL))
                .toList();
    }

    private static BloodTestResultRequest validResult() {
        return new BloodTestResultRequest("Example Analyte", "10.5", "unit", "5-15", BloodTestResultFlag.NORMAL);
    }

    private static BloodTestRequests requests(
            String title,
            LocalDate testDate,
            String provider,
            String notes,
            List<BloodTestResultRequest> results) {
        return new BloodTestRequests(
                new CreateBloodTestRequest(title, testDate, provider, notes, results),
                new UpdateBloodTestRequest(title, testDate, provider, notes, results));
    }

    private static void assertBothValid(
            BloodTestRequests requests) {
        assertValid(requests.createRequest());
        assertValid(requests.updateRequest());
    }

    private static void assertBothInvalidField(BloodTestRequests requests, String field) {
        assertInvalidField(requests.createRequest(), field);
        assertInvalidField(requests.updateRequest(), field);
    }

    private record BloodTestRequests(CreateBloodTestRequest createRequest, UpdateBloodTestRequest updateRequest) {
    }
}