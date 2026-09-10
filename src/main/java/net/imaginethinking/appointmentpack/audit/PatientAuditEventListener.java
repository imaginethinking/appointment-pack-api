package net.imaginethinking.appointmentpack.audit;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saves patient activity events as part of the transaction that published them.
 */
@Component
@RequiredArgsConstructor
public class PatientAuditEventListener {

    private final PatientAuditEventRepository patientAuditEventRepository;

    /**
     * Creates and saves an audit record for the patient activity event using the current transaction.
     */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void handle(PatientActivityEvent event) {
        PatientAuditEvent auditEvent = new PatientAuditEvent(
                event.eventId(),
                event.patientRecordId(),
                event.actorUserId(),
                event.resourceType(),
                event.resourceId(),
                event.action(),
                event.occurredAt()
        );

        patientAuditEventRepository.saveAndFlush(auditEvent);
    }
}