package net.imaginethinking.appointmentpack.audit;

import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PatientAuditEventListenerTest {

    @Mock
    private PatientAuditEventRepository patientAuditEventRepository;

    private PatientAuditEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new PatientAuditEventListener(patientAuditEventRepository);
    }

    @Test
    void shouldPersistPatientActivityAsImmutableAuditEvent() {
        UUID eventId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-14T04:00:00Z");

        PatientActivityEvent event = new PatientActivityEvent(
                eventId,
                occurredAt,
                actorUserId,
                patientRecordId,
                PatientResourceType.DOCUMENT,
                resourceId,
                PatientActivityAction.DOWNLOADED);

        listener.handle(event);

        ArgumentCaptor<PatientAuditEvent> captor = ArgumentCaptor.forClass(PatientAuditEvent.class);

        verify(patientAuditEventRepository).saveAndFlush(captor.capture());

        PatientAuditEvent persisted = captor.getValue();

        assertEquals(eventId, persisted.getSourceEventId());
        assertEquals(patientRecordId, persisted.getPatientRecordId());
        assertEquals(actorUserId, persisted.getActorUserId());
        assertEquals(PatientResourceType.DOCUMENT, persisted.getResourceType());
        assertEquals(resourceId, persisted.getResourceId());
        assertEquals(PatientActivityAction.DOWNLOADED, persisted.getAction());
        assertEquals(occurredAt, persisted.getOccurredAt());
    }
}