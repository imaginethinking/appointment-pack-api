package net.imaginethinking.appointmentpack.analytics.admin;

import net.imaginethinking.appointmentpack.event.analytics.ApplicationPage;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;

import java.time.Instant;
import java.util.List;

/**
 * Represents admin analytics summary information returned by the API.
 */
public record AdminAnalyticsSummaryResponse(
        Instant from,
        Instant to,
        long totalOperationalEvents,
        UserMetrics users,
        AuthenticationMetrics authentication,
        DocumentProcessingMetrics documentProcessing,
        PatientActivityMetrics patientActivity,
        PageViewMetrics pageViews
) {

    /**
     * Groups the user counts shown in the admin analytics summary.
     */
    public record UserMetrics(
            long totalUsers,
            long registeredInPeriod,
            long activeUsersInPeriod
    ) {
    }

    /**
     * Groups authentication and account security counts shown in the admin analytics summary.
     */
    public record AuthenticationMetrics(
            long loginAttempts,
            long authenticatedSessions,
            long passwordLoginSucceeded,
            long loginFailed,
            long loginBlocked,
            long mfaRequired,
            long mfaChallengesCreated,
            long mfaLoginSucceeded,
            long mfaLoginFailed,
            long mfaSetupEnabled,
            long emailVerificationRequested,
            long emailVerificationResent,
            long emailVerificationSucceeded,
            long emailVerificationFailed,
            long passwordResetRequested,
            long passwordResetSucceeded,
            long passwordResetFailed
    ) {
    }

    /**
     * Groups extraction and summarisation counts, failures and timings for admin analytics.
     */
    public record DocumentProcessingMetrics(
            long extractionSucceeded,
            long extractionFailed,
            Long averageSuccessfulExtractionDurationMs,
            long aiSummarisationSucceeded,
            long aiSummarisationFailed,
            Long averageSuccessfulAiSummarisationDurationMs,
            long timeoutFailures,
            long serviceUnavailableFailures,
            long processingErrorFailures,
            long unknownFailures
    ) {
    }

    /**
     * Groups patient activity counts by resource and action.
     */
    public record PatientActivityMetrics(
            long total,
            List<PatientActivityCount> breakdown
    ) {
        /**
         * Copies the patient activity counts before storing them in the analytics summary.
         */
        public PatientActivityMetrics {
            breakdown = List.copyOf(breakdown);
        }
    }

    /**
     * Keeps one resource, action and count returned in the patient activity summary.
     */
    public record PatientActivityCount(
            PatientResourceType resourceType,
            PatientActivityAction action,
            long count
    ) {
    }

    /**
     * Groups page view counts for the admin analytics summary.
     */
    public record PageViewMetrics(
            long total,
            List<PageViewCount> breakdown
    ) {
        /**
         * Copies the page view counts before storing them in the analytics summary.
         */
        public PageViewMetrics {
            breakdown = List.copyOf(breakdown);
        }
    }

    /**
     * Keeps one application page and its recorded view count.
     */
    public record PageViewCount(
            ApplicationPage page,
            long count
    ) {
    }
}