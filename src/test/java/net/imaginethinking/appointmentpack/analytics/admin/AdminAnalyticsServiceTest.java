package net.imaginethinking.appointmentpack.analytics.admin;

import net.imaginethinking.appointmentpack.analytics.*;
import net.imaginethinking.appointmentpack.event.analytics.ApplicationPage;
import net.imaginethinking.appointmentpack.event.analytics.PageViewedEvent;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationAction;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationOutcome;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingFailureReason;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingOperation;
import net.imaginethinking.appointmentpack.event.processing.ProcessingOutcome;
import net.imaginethinking.appointmentpack.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsServiceTest {

    @Mock
    private OperationalEventRepository operationalEventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientActivityCountProjection patientActivityProjection;

    @Mock
    private PageViewCountProjection pageViewProjection;

    private AdminAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AdminAnalyticsService(operationalEventRepository, userRepository);
    }

    @Test
    void shouldBuildSummaryFromOperationalMetrics() {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-14T00:00:00Z");

        when(operationalEventRepository.countInRange(from, to)).thenReturn(42L);

        when(userRepository.count()).thenReturn(10L);

        when(operationalEventRepository.countAuthenticationInRange(
                AuthenticationAction.REGISTRATION,
                AuthenticationOutcome.SUCCEEDED,
                from,
                to)).thenReturn(3L);

        when(operationalEventRepository.countDistinctActiveUsersInRange(
                Set.of(
                        OperationalEventCategory.PATIENT_ACTIVITY,
                        OperationalEventCategory.DOCUMENT_PROCESSING,
                        OperationalEventCategory.PAGE_VIEW),
                Set.of(AuthenticationAction.LOGIN, AuthenticationAction.MFA_LOGIN),
                AuthenticationOutcome.SUCCEEDED,
                from,
                to)).thenReturn(6L);

        when(operationalEventRepository.countAuthenticationActionInRange(
                AuthenticationAction.LOGIN,
                from,
                to)).thenReturn(9L);

        when(operationalEventRepository.countAuthenticationInRange(
                AuthenticationAction.LOGIN,
                AuthenticationOutcome.SUCCEEDED,
                from,
                to)).thenReturn(4L);

        when(operationalEventRepository.countAuthenticationInRange(
                AuthenticationAction.MFA_LOGIN,
                AuthenticationOutcome.SUCCEEDED,
                from,
                to)).thenReturn(2L);

        when(operationalEventRepository.countAuthenticationInRange(
                AuthenticationAction.LOGIN,
                AuthenticationOutcome.FAILED,
                from,
                to)).thenReturn(2L);

        when(operationalEventRepository.countProcessingInRange(
                DocumentProcessingOperation.EXTRACTION,
                ProcessingOutcome.SUCCEEDED,
                from,
                to)).thenReturn(5L);

        when(operationalEventRepository.countProcessingInRange(
                DocumentProcessingOperation.EXTRACTION,
                ProcessingOutcome.FAILED,
                from,
                to)).thenReturn(1L);

        when(operationalEventRepository.averageProcessingDurationInRange(
                DocumentProcessingOperation.EXTRACTION,
                ProcessingOutcome.SUCCEEDED,
                from,
                to)).thenReturn(120.6);

        when(operationalEventRepository.countProcessingInRange(
                DocumentProcessingOperation.AI_SUMMARISATION,
                ProcessingOutcome.SUCCEEDED,
                from,
                to)).thenReturn(3L);

        when(operationalEventRepository.countProcessingInRange(
                DocumentProcessingOperation.AI_SUMMARISATION,
                ProcessingOutcome.FAILED,
                from,
                to)).thenReturn(1L);

        when(operationalEventRepository.averageProcessingDurationInRange(
                DocumentProcessingOperation.AI_SUMMARISATION,
                ProcessingOutcome.SUCCEEDED,
                from,
                to)).thenReturn(250.4);

        when(operationalEventRepository.countProcessingFailureReasonInRange(
                DocumentProcessingFailureReason.TIMEOUT,
                from,
                to)).thenReturn(2L);

        when(patientActivityProjection.getResourceType()).thenReturn(PatientResourceType.APPOINTMENT);
        when(patientActivityProjection.getAction()).thenReturn(PatientActivityAction.CREATED);
        when(patientActivityProjection.getEventCount()).thenReturn(4L);

        when(operationalEventRepository.countPatientActivityInRange(
                OperationalEventCategory.PATIENT_ACTIVITY,
                from,
                to)).thenReturn(List.of(patientActivityProjection));

        when(pageViewProjection.getPage()).thenReturn(ApplicationPage.DASHBOARD);
        when(pageViewProjection.getEventCount()).thenReturn(5L);

        when(operationalEventRepository.countPageViewsInRange(OperationalEventCategory.PAGE_VIEW, from, to)).thenReturn(
                List.of(pageViewProjection));

        AdminAnalyticsSummaryResponse response = service.getSummary(from, to);

        assertEquals(from, response.from());
        assertEquals(to, response.to());
        assertEquals(42L, response.totalOperationalEvents());

        assertEquals(10L, response.users().totalUsers());
        assertEquals(3L, response.users().registeredInPeriod());
        assertEquals(6L, response.users().activeUsersInPeriod());
        assertEquals(9L, response.authentication().loginAttempts());
        assertEquals(6L, response.authentication().authenticatedSessions());
        assertEquals(4L, response.authentication().passwordLoginSucceeded());
        assertEquals(2L, response.authentication().mfaLoginSucceeded());
        assertEquals(2L, response.authentication().loginFailed());

        assertEquals(5L, response.documentProcessing().extractionSucceeded());
        assertEquals(1L, response.documentProcessing().extractionFailed());
        assertEquals(121L, response.documentProcessing().averageSuccessfulExtractionDurationMs());
        assertEquals(3L, response.documentProcessing().aiSummarisationSucceeded());
        assertEquals(250L, response.documentProcessing().averageSuccessfulAiSummarisationDurationMs());
        assertEquals(2L, response.documentProcessing().timeoutFailures());

        assertEquals(4L, response.patientActivity().total());
        assertEquals(PatientResourceType.APPOINTMENT, response.patientActivity().breakdown().getFirst().resourceType());

        assertEquals(5L, response.pageViews().total());
        assertEquals(ApplicationPage.DASHBOARD, response.pageViews().breakdown().getFirst().page());
    }

    @Test
    void shouldAcceptAnalyticsRangeImmediatelyBeforeTo() {
        Instant to = Instant.parse("2026-08-14T00:00:00Z");
        Instant from = to.minusNanos(1);

        PageRequest pageRequest = PageRequest.of(0, 1);

        when(operationalEventRepository.findEventsInRange(from, to, pageRequest)).thenReturn(new PageImpl<>(
                List.of(),
                pageRequest,
                0));

        OperationalEventPageResponse response = service.getEvents(from, to, null, 0, 1);

        assertEquals(0, response.page());
        assertEquals(1, response.size());

        verify(operationalEventRepository).findEventsInRange(from, to, pageRequest);
    }

    @Test
    void shouldRejectAnalyticsRangeWhenFromEqualsTo() {
        Instant boundary = Instant.parse("2026-08-14T00:00:00Z");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getSummary(boundary, boundary));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verifyNoInteractions(operationalEventRepository, userRepository);
    }

    @Test
    void shouldRejectAnalyticsRangeWhenFromIsAfterTo() {
        Instant to = Instant.parse("2026-08-14T00:00:00Z");
        Instant from = to.plusNanos(1);
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getEvents(from, to, null, 0, 20));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verifyNoInteractions(operationalEventRepository);
    }

    @Test
    void shouldUseThirtyDayDefaultWhenFromIsOmitted() {
        Instant to = Instant.parse("2026-08-14T00:00:00Z");

        Instant expectedFrom = to.minus(Duration.ofDays(30));

        PageRequest pageRequest = PageRequest.of(0, 20);

        when(operationalEventRepository.findEventsInRange(
                expectedFrom,
                to,
                pageRequest)).thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        service.getEvents(null, to, null, 0, 20);

        verify(operationalEventRepository).findEventsInRange(expectedFrom, to, pageRequest);
    }

    @Test
    void shouldUseCurrentTimeWhenToIsOmitted() {
        Instant from = Instant.now().minus(Duration.ofDays(1));

        PageRequest pageRequest = PageRequest.of(0, 20);

        when(operationalEventRepository.findEventsInRange(
                eq(from),
                any(Instant.class),
                eq(pageRequest))).thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        Instant before = Instant.now();

        service.getEvents(from, null, null, 0, 20);

        Instant after = Instant.now();

        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(operationalEventRepository).findEventsInRange(eq(from), toCaptor.capture(), eq(pageRequest));

        Instant resolvedTo = toCaptor.getValue();

        assertFalse(resolvedTo.isBefore(before));
        assertFalse(resolvedTo.isAfter(after));
        assertFalse(resolvedTo.isBefore(from));
    }

    @Test
    void shouldUseThirtyDayDefaultRangeWhenBothBoundsAreOmitted() {
        PageRequest pageRequest = PageRequest.of(0, 20);

        when(operationalEventRepository.findEventsInRange(
                any(Instant.class),
                any(Instant.class),
                eq(pageRequest))).thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        Instant before = Instant.now();

        service.getEvents(null, null, null, 0, 20);

        Instant after = Instant.now();

        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);

        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(operationalEventRepository).findEventsInRange(fromCaptor.capture(), toCaptor.capture(), eq(pageRequest));

        Instant resolvedFrom = fromCaptor.getValue();
        Instant resolvedTo = toCaptor.getValue();

        assertEquals(Duration.ofDays(30), Duration.between(resolvedFrom, resolvedTo));

        assertFalse(resolvedTo.isBefore(before));
        assertFalse(resolvedTo.isAfter(after));
    }

    @Test
    void shouldReturnUnfilteredOperationalEventPage() {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-14T00:00:00Z");
        PageRequest pageRequest = PageRequest.of(0, 20);

        OperationalEvent event = new OperationalEventFactory().from(new PageViewedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-08-10T12:00:00Z"),
                UUID.randomUUID(),
                ApplicationPage.DOCUMENTS));

        when(operationalEventRepository.findEventsInRange(from, to, pageRequest)).thenReturn(new PageImpl<>(
                List.of(event), pageRequest, 1));

        OperationalEventPageResponse response = service.getEvents(from, to, null, 0, 20);

        assertEquals(1, response.events().size());
        assertEquals(OperationalEventCategory.PAGE_VIEW, response.events().getFirst().category());
        assertEquals(ApplicationPage.DOCUMENTS, response.events().getFirst().page());
    }

    @Test
    void shouldUseCategorySpecificQueryWhenFilterIsProvided() {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-14T00:00:00Z");
        PageRequest pageRequest = PageRequest.of(1, 10);

        when(operationalEventRepository.findEventsByCategoryInRange(
                OperationalEventCategory.AUTHENTICATION,
                from,
                to,
                pageRequest)).thenReturn(new PageImpl<>(List.of(), pageRequest, 15));

        OperationalEventPageResponse response = service.getEvents(
                from,
                to,
                OperationalEventCategory.AUTHENTICATION,
                1,
                10);

        assertEquals(1, response.page());
        assertEquals(10, response.size());
        assertEquals(15, response.totalElements());
        assertEquals(2, response.totalPages());

        verify(operationalEventRepository).findEventsByCategoryInRange(
                OperationalEventCategory.AUTHENTICATION,
                from,
                to,
                pageRequest);
    }
}