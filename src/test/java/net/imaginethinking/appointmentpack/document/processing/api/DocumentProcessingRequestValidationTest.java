package net.imaginethinking.appointmentpack.document.processing.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.*;

class DocumentProcessingRequestValidationTest {

    @Test
    void shouldAcceptRepresentativeSummarisationRequest() {
        assertValid(new DocumentSummarisationRequest("Approved de-identified consultation text"));
    }

    @Test
    void shouldEnforceApprovedDeidentifiedTextLengthBoundary() {
        assertValid(new DocumentSummarisationRequest(stringOfLength(99_999)));

        assertValid(new DocumentSummarisationRequest(stringOfLength(100_000)));

        assertInvalidField(new DocumentSummarisationRequest(stringOfLength(100_001)), "approvedDeidentifiedText");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectBlankApprovedDeidentifiedText(String text) {
        assertInvalidField(new DocumentSummarisationRequest(text), "approvedDeidentifiedText");
    }

    @Test
    void shouldAcceptRepresentativeSummaryAcceptanceRequest() {
        assertValid(new DocumentSummaryAcceptanceRequest(
                "Reviewed consultation summary",
                "Consultation outcome",
                LocalDate.now()));
    }

    @Test
    void shouldEnforceReviewedSummaryLengthBoundary() {
        assertValid(summaryAcceptanceWithReviewedSummary(stringOfLength(9_999)));

        assertValid(summaryAcceptanceWithReviewedSummary(stringOfLength(10_000)));

        assertInvalidField(summaryAcceptanceWithReviewedSummary(stringOfLength(10_001)), "reviewedSummary");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectBlankReviewedSummary(String summary) {
        assertInvalidField(summaryAcceptanceWithReviewedSummary(summary), "reviewedSummary");
    }

    @Test
    void shouldEnforceHistoryTitleLengthBoundary() {
        assertValid(summaryAcceptanceWithHistoryTitle(stringOfLength(199)));

        assertValid(summaryAcceptanceWithHistoryTitle(stringOfLength(200)));

        assertInvalidField(summaryAcceptanceWithHistoryTitle(stringOfLength(201)), "historyTitle");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectBlankHistoryTitle(String title) {
        assertInvalidField(summaryAcceptanceWithHistoryTitle(title), "historyTitle");
    }

    @Test
    void shouldAcceptPastHistoryDate() {
        assertValid(summaryAcceptanceWithHistoryDate(LocalDate.now().minusDays(1)));
    }

    @Test
    void shouldAcceptPresentHistoryDate() {
        assertValid(summaryAcceptanceWithHistoryDate(LocalDate.now()));
    }

    @Test
    void shouldRejectFutureHistoryDate() {
        assertInvalidField(summaryAcceptanceWithHistoryDate(LocalDate.now().plusDays(1)), "historyDate");
    }

    @Test
    void shouldRejectNullHistoryDate() {
        assertInvalidField(summaryAcceptanceWithHistoryDate(null), "historyDate");
    }

    private static DocumentSummaryAcceptanceRequest summaryAcceptanceWithReviewedSummary(
            String reviewedSummary) {
        return new DocumentSummaryAcceptanceRequest(reviewedSummary, "Consultation outcome", LocalDate.now());
    }

    private static DocumentSummaryAcceptanceRequest summaryAcceptanceWithHistoryTitle(
            String historyTitle) {
        return new DocumentSummaryAcceptanceRequest("Reviewed consultation summary", historyTitle, LocalDate.now());
    }

    private static DocumentSummaryAcceptanceRequest summaryAcceptanceWithHistoryDate(
            LocalDate historyDate) {
        return new DocumentSummaryAcceptanceRequest(
                "Reviewed consultation summary",
                "Consultation outcome",
                historyDate);
    }
}