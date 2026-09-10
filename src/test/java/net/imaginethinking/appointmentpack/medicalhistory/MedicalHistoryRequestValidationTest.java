package net.imaginethinking.appointmentpack.medicalhistory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.*;

/**
 * Checks the validation rules used for medical history request.
 */
class MedicalHistoryRequestValidationTest {

    @Test
    void shouldAcceptRepresentativeMedicalHistoryRequests() {
        MedicalHistoryRequests requests = requests(
                "Example history entry",
                "Example clinical history summary.",
                LocalDate.now());

        assertBothValid(requests);
    }

    @Test
    void shouldEnforceMedicalHistoryTitleBoundary() {
        assertBothValid(withTitle(stringOfLength(199)));
        assertBothValid(withTitle(stringOfLength(200)));

        assertBothInvalidField(withTitle(stringOfLength(201)), "title");
    }

    @Test
    void shouldEnforceMedicalHistorySummaryBoundary() {
        assertBothValid(withSummary(stringOfLength(9999)));
        assertBothValid(withSummary(stringOfLength(10000)));

        assertBothInvalidField(withSummary(stringOfLength(10001)), "summary");
    }

    @ParameterizedTest
    @MethodSource("invalidRequiredTextValues")
    void shouldRejectBlankOrNullRequiredMedicalHistoryText(String field, String value) {
        switch (field) {
            case "title" -> assertBothInvalidField(withTitle(value), field);
            case "summary" -> assertBothInvalidField(withSummary(value), field);
            default -> throw new IllegalArgumentException("Unsupported medical history field: " + field);
        }
    }

    @Test
    void shouldAcceptPastMedicalHistoryDate() {
        assertBothValid(withDate(LocalDate.now().minusDays(1)));
    }

    @Test
    void shouldAcceptPresentMedicalHistoryDate() {
        assertBothValid(withDate(LocalDate.now()));
    }

    @Test
    void shouldRejectFutureMedicalHistoryDate() {
        assertBothInvalidField(withDate(LocalDate.now().plusDays(1)), "entryDate");
    }

    @Test
    void shouldRejectNullMedicalHistoryDate() {
        assertBothInvalidField(withDate(null), "entryDate");
    }

    /**
     * Provides invalid required text values values used by the parameterised validation tests.
     */
    private static Stream<Arguments> invalidRequiredTextValues() {
        return Stream.of(
                Arguments.of("title", ""),
                Arguments.of("title", "   "),
                Arguments.of("title", null),
                Arguments.of("summary", ""),
                Arguments.of("summary", "   "),
                Arguments.of("summary", null));
    }

    /**
     * Returns a test request with the title value replaced by the supplied value.
     */
    private static MedicalHistoryRequests withTitle(String title) {
        return requests(title, "Example summary", LocalDate.now());
    }

    /**
     * Returns a test request with the summary value replaced by the supplied value.
     */
    private static MedicalHistoryRequests withSummary(String summary) {
        return requests("Example title", summary, LocalDate.now());
    }

    /**
     * Returns a test request with the date value replaced by the supplied value.
     */
    private static MedicalHistoryRequests withDate(LocalDate entryDate) {
        return requests("Example title", "Example summary", entryDate);
    }

    /**
     * Creates the create and update request variants using the supplied values.
     */
    private static MedicalHistoryRequests requests(String title, String summary, LocalDate entryDate) {
        return new MedicalHistoryRequests(
                new CreateMedicalHistoryEntryRequest(title, summary, entryDate),
                new UpdateMedicalHistoryEntryRequest(title, summary, entryDate));
    }

    /**
     * Checks that both request variants pass validation.
     */
    private static void assertBothValid(
            MedicalHistoryRequests requests) {
        assertValid(requests.createRequest());
        assertValid(requests.updateRequest());
    }

    /**
     * Checks that both request variants report a validation error for the given field.
     */
    private static void assertBothInvalidField(MedicalHistoryRequests requests, String field) {
        assertInvalidField(requests.createRequest(), field);
        assertInvalidField(requests.updateRequest(), field);
    }

    /**
     * Checks the medical history requests behaviour covered by this test class.
     */
    private record MedicalHistoryRequests(CreateMedicalHistoryEntryRequest createRequest,
                                          UpdateMedicalHistoryEntryRequest updateRequest) {
    }
}