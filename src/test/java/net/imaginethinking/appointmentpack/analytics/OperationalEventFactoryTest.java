package net.imaginethinking.appointmentpack.analytics;

import net.imaginethinking.appointmentpack.document.DocumentType;
import net.imaginethinking.appointmentpack.event.analytics.ApplicationPage;
import net.imaginethinking.appointmentpack.event.analytics.PageViewedEvent;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationAction;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationEvent;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationOutcome;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingEvent;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingFailureReason;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingOperation;
import net.imaginethinking.appointmentpack.event.processing.ProcessingOutcome;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OperationalEventFactoryTest {

    private final OperationalEventFactory factory = new OperationalEventFactory();

    @Test
    void shouldProjectPatientActivityWithoutClinicalPayload() {
        UUID eventId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-14T04:00:00Z");

        PatientActivityEvent source = new PatientActivityEvent(
                eventId,
                occurredAt,
                actorUserId,
                UUID.randomUUID(),
                PatientResourceType.MEDICATION,
                UUID.randomUUID(),
                PatientActivityAction.UPDATED);

        OperationalEvent event = factory.from(source);

        assertEquals(eventId, event.getSourceEventId());
        assertEquals(OperationalEventCategory.PATIENT_ACTIVITY, event.getCategory());
        assertEquals("MEDICATION_UPDATED", event.getEventName());
        assertEquals(actorUserId, event.getUserId());
        assertEquals(occurredAt, event.getOccurredAt());
        assertEquals(PatientResourceType.MEDICATION, event.getPatientResourceType());
        assertEquals(PatientActivityAction.UPDATED, event.getPatientAction());
        assertNull(event.getDocumentType());
        assertNull(event.getPage());
    }

    @Test
    void shouldProjectAuthenticationEvent() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        AuthenticationEvent source = new AuthenticationEvent(
                eventId,
                Instant.parse("2026-08-14T04:01:00Z"),
                userId,
                AuthenticationAction.LOGIN,
                AuthenticationOutcome.SUCCEEDED);

        OperationalEvent event = factory.from(source);

        assertEquals(OperationalEventCategory.AUTHENTICATION, event.getCategory());
        assertEquals("LOGIN_SUCCEEDED", event.getEventName());
        assertEquals(userId, event.getUserId());
        assertEquals(AuthenticationAction.LOGIN, event.getAuthenticationAction());
        assertEquals(AuthenticationOutcome.SUCCEEDED, event.getAuthenticationOutcome());
    }

    @Test
    void shouldProjectDocumentProcessingEvent() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        DocumentProcessingEvent source = new DocumentProcessingEvent(
                eventId,
                Instant.parse("2026-08-14T04:02:00Z"),
                userId,
                UUID.randomUUID(),
                DocumentType.CONSULTATION_OUTCOME_LETTER,
                DocumentProcessingOperation.AI_SUMMARISATION,
                ProcessingOutcome.FAILED,
                450L,
                DocumentProcessingFailureReason.TIMEOUT,
                null,
                null,
                null);

        OperationalEvent event = factory.from(source);

        assertEquals(OperationalEventCategory.DOCUMENT_PROCESSING, event.getCategory());
        assertEquals("AI_SUMMARISATION_FAILED", event.getEventName());
        assertEquals(DocumentType.CONSULTATION_OUTCOME_LETTER, event.getDocumentType());
        assertEquals(DocumentProcessingOperation.AI_SUMMARISATION, event.getProcessingOperation());
        assertEquals(ProcessingOutcome.FAILED, event.getProcessingOutcome());
        assertEquals(DocumentProcessingFailureReason.TIMEOUT, event.getProcessingFailureReason());
        assertEquals(450L, event.getDurationMs());
    }

    @Test
    void shouldProjectPageViewEvent() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PageViewedEvent source = new PageViewedEvent(
                eventId,
                Instant.parse("2026-08-14T04:03:00Z"),
                userId,
                ApplicationPage.DOCUMENTS);

        OperationalEvent event = factory.from(source);

        assertEquals(OperationalEventCategory.PAGE_VIEW, event.getCategory());
        assertEquals("PAGE_VIEWED", event.getEventName());
        assertEquals(ApplicationPage.DOCUMENTS, event.getPage());
        assertEquals(userId, event.getUserId());
    }
}