package net.imaginethinking.appointmentpack.analytics.admin;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.analytics.*;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationAction;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationOutcome;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingFailureReason;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingOperation;
import net.imaginethinking.appointmentpack.event.processing.ProcessingOutcome;
import net.imaginethinking.appointmentpack.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private static final Duration DEFAULT_RANGE = Duration.ofDays(30);

    private final OperationalEventRepository operationalEventRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AdminAnalyticsSummaryResponse getSummary(Instant from, Instant to) {
        AnalyticsRange range = resolveRange(from, to);

        return new AdminAnalyticsSummaryResponse(
                range.from(),
                range.to(),
                operationalEventRepository.countInRange(range.from(), range.to()),
                buildUserMetrics(range),
                buildAuthenticationMetrics(range),
                buildDocumentProcessingMetrics(range),
                buildPatientActivityMetrics(range),
                buildPageViewMetrics(range));
    }

    @Transactional(readOnly = true)
    public OperationalEventPageResponse getEvents(
            Instant from,
            Instant to,
            OperationalEventCategory category,
            int page,
            int size) {
        AnalyticsRange range = resolveRange(from, to);
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<OperationalEvent> eventPage = category == null ? operationalEventRepository.findEventsInRange(
                range.from(),
                range.to(),
                pageRequest) : operationalEventRepository.findEventsByCategoryInRange(
                category,
                range.from(),
                range.to(),
                pageRequest);

        return new OperationalEventPageResponse(
                eventPage.getContent().stream().map(OperationalEventResponse::from).toList(),
                eventPage.getNumber(),
                eventPage.getSize(),
                eventPage.getTotalElements(),
                eventPage.getTotalPages());
    }

    private AdminAnalyticsSummaryResponse.UserMetrics buildUserMetrics(AnalyticsRange range) {
        return new AdminAnalyticsSummaryResponse.UserMetrics(
                userRepository.count(),
                countAuthentication(AuthenticationAction.REGISTRATION, AuthenticationOutcome.SUCCEEDED, range),
                operationalEventRepository.countDistinctActiveUsersInRange(
                        Set.of(
                                OperationalEventCategory.PATIENT_ACTIVITY,
                                OperationalEventCategory.DOCUMENT_PROCESSING,
                                OperationalEventCategory.PAGE_VIEW),
                        Set.of(AuthenticationAction.LOGIN, AuthenticationAction.MFA_LOGIN),
                        AuthenticationOutcome.SUCCEEDED,
                        range.from(),
                        range.to()));
    }

    private AdminAnalyticsSummaryResponse.AuthenticationMetrics buildAuthenticationMetrics(AnalyticsRange range) {
        long passwordLoginSucceeded = countAuthentication(
                AuthenticationAction.LOGIN,
                AuthenticationOutcome.SUCCEEDED,
                range);

        long mfaLoginSucceeded = countAuthentication(
                AuthenticationAction.MFA_LOGIN,
                AuthenticationOutcome.SUCCEEDED,
                range);

        return new AdminAnalyticsSummaryResponse.AuthenticationMetrics(
                operationalEventRepository.countAuthenticationActionInRange(
                        AuthenticationAction.LOGIN,
                        range.from(),
                        range.to()),
                passwordLoginSucceeded + mfaLoginSucceeded,
                passwordLoginSucceeded,
                countAuthentication(AuthenticationAction.LOGIN, AuthenticationOutcome.FAILED, range),
                countAuthentication(AuthenticationAction.LOGIN, AuthenticationOutcome.BLOCKED, range),
                countAuthentication(AuthenticationAction.LOGIN, AuthenticationOutcome.MFA_REQUIRED, range),
                countAuthentication(AuthenticationAction.MFA_CHALLENGE, AuthenticationOutcome.CREATED, range),
                mfaLoginSucceeded,
                countAuthentication(AuthenticationAction.MFA_LOGIN, AuthenticationOutcome.FAILED, range),
                countAuthentication(AuthenticationAction.MFA_SETUP, AuthenticationOutcome.ENABLED, range),
                countAuthentication(AuthenticationAction.EMAIL_VERIFICATION, AuthenticationOutcome.REQUESTED, range),
                countAuthentication(AuthenticationAction.EMAIL_VERIFICATION, AuthenticationOutcome.RESENT, range),
                countAuthentication(AuthenticationAction.EMAIL_VERIFICATION, AuthenticationOutcome.SUCCEEDED, range),
                countAuthentication(AuthenticationAction.EMAIL_VERIFICATION, AuthenticationOutcome.FAILED, range),
                countAuthentication(AuthenticationAction.PASSWORD_RESET, AuthenticationOutcome.REQUESTED, range),
                countAuthentication(AuthenticationAction.PASSWORD_RESET, AuthenticationOutcome.SUCCEEDED, range),
                countAuthentication(AuthenticationAction.PASSWORD_RESET, AuthenticationOutcome.FAILED, range));
    }

    private AdminAnalyticsSummaryResponse.DocumentProcessingMetrics buildDocumentProcessingMetrics(AnalyticsRange range) {
        return new AdminAnalyticsSummaryResponse.DocumentProcessingMetrics(
                countProcessing(DocumentProcessingOperation.EXTRACTION, ProcessingOutcome.SUCCEEDED, range),
                countProcessing(DocumentProcessingOperation.EXTRACTION, ProcessingOutcome.FAILED, range),
                averageProcessingDuration(DocumentProcessingOperation.EXTRACTION, ProcessingOutcome.SUCCEEDED, range),
                countProcessing(DocumentProcessingOperation.AI_SUMMARISATION, ProcessingOutcome.SUCCEEDED, range),
                countProcessing(DocumentProcessingOperation.AI_SUMMARISATION, ProcessingOutcome.FAILED, range),
                averageProcessingDuration(
                        DocumentProcessingOperation.AI_SUMMARISATION,
                        ProcessingOutcome.SUCCEEDED,
                        range),
                countProcessingFailure(DocumentProcessingFailureReason.TIMEOUT, range),
                countProcessingFailure(DocumentProcessingFailureReason.SERVICE_UNAVAILABLE, range),
                countProcessingFailure(DocumentProcessingFailureReason.PROCESSING_ERROR, range),
                countProcessingFailure(DocumentProcessingFailureReason.UNKNOWN, range));
    }

    private AdminAnalyticsSummaryResponse.PatientActivityMetrics buildPatientActivityMetrics(AnalyticsRange range) {
        List<PatientActivityCountProjection> counts = operationalEventRepository.countPatientActivityInRange(OperationalEventCategory.PATIENT_ACTIVITY,
                range.from(),
                range.to());

        long total = counts.stream().mapToLong(PatientActivityCountProjection::getEventCount).sum();

        List<AdminAnalyticsSummaryResponse.PatientActivityCount> breakdown = counts.stream()
                .map(item -> new AdminAnalyticsSummaryResponse.PatientActivityCount(
                        item.getResourceType(),
                        item.getAction(),
                        item.getEventCount()))
                .toList();

        return new AdminAnalyticsSummaryResponse.PatientActivityMetrics(total, breakdown);
    }

    private AdminAnalyticsSummaryResponse.PageViewMetrics buildPageViewMetrics(AnalyticsRange range) {
        List<PageViewCountProjection> counts = operationalEventRepository.countPageViewsInRange(
                OperationalEventCategory.PAGE_VIEW,
                range.from(),
                range.to());

        long total = counts.stream().mapToLong(PageViewCountProjection::getEventCount).sum();

        List<AdminAnalyticsSummaryResponse.PageViewCount> breakdown = counts.stream()
                .map(item -> new AdminAnalyticsSummaryResponse.PageViewCount(item.getPage(), item.getEventCount()))
                .toList();

        return new AdminAnalyticsSummaryResponse.PageViewMetrics(total, breakdown);
    }

    private long countAuthentication(AuthenticationAction action, AuthenticationOutcome outcome, AnalyticsRange range) {
        return operationalEventRepository.countAuthenticationInRange(action, outcome, range.from(), range.to());
    }

    private long countProcessing(
            DocumentProcessingOperation operation,
            ProcessingOutcome outcome,
            AnalyticsRange range) {
        return operationalEventRepository.countProcessingInRange(operation, outcome, range.from(), range.to());
    }

    private long countProcessingFailure(DocumentProcessingFailureReason reason, AnalyticsRange range) {
        return operationalEventRepository.countProcessingFailureReasonInRange(reason, range.from(), range.to());
    }

    private Long averageProcessingDuration(
            DocumentProcessingOperation operation,
            ProcessingOutcome outcome,
            AnalyticsRange range) {
        Double average = operationalEventRepository.averageProcessingDurationInRange(
                operation,
                outcome,
                range.from(),
                range.to());

        return average == null ? null : Math.round(average);
    }

    private AnalyticsRange resolveRange(Instant from, Instant to) {
        Instant resolvedTo = to == null ? Instant.now() : to;

        Instant resolvedFrom = from == null ? resolvedTo.minus(DEFAULT_RANGE) : from;

        if (!resolvedFrom.isBefore(resolvedTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Analytics 'from' must be before 'to'");
        }

        return new AnalyticsRange(resolvedFrom, resolvedTo);
    }

    private record AnalyticsRange(Instant from, Instant to) {
    }
}