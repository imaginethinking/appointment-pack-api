package net.imaginethinking.appointmentpack.analytics.admin;

import net.imaginethinking.appointmentpack.event.analytics.ApplicationPage;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;

import java.time.Instant;
import java.util.List;

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

    public record UserMetrics(
            long totalUsers,
            long registeredInPeriod,
            long activeUsersInPeriod
    ) {
    }

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

    public record PatientActivityMetrics(
            long total,
            List<PatientActivityCount> breakdown
    ) {
        public PatientActivityMetrics {
            breakdown = List.copyOf(breakdown);
        }
    }

    public record PatientActivityCount(
            PatientResourceType resourceType,
            PatientActivityAction action,
            long count
    ) {
    }

    public record PageViewMetrics(
            long total,
            List<PageViewCount> breakdown
    ) {
        public PageViewMetrics {
            breakdown = List.copyOf(breakdown);
        }
    }

    public record PageViewCount(
            ApplicationPage page,
            long count
    ) {
    }
}