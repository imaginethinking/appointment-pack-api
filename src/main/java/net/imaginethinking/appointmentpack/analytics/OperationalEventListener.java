package net.imaginethinking.appointmentpack.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.imaginethinking.appointmentpack.event.analytics.PageViewedEvent;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationEvent;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Converts application events into operational analytics once their surrounding transaction has completed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationalEventListener {

    private final OperationalEventFactory operationalEventFactory;
    private final OperationalEventRecorder operationalEventRecorder;

    /**
     * Records the patient activity as operational analytics after its transaction commits.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(PatientActivityEvent event) {
        recordSafely(operationalEventFactory.from(event));
    }

    /**
     * Records the authentication event as operational analytics after its transaction commits.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(AuthenticationEvent event) {
        recordSafely(operationalEventFactory.from(event));
    }

    /**
     * Records the document processing event as operational analytics after its transaction commits.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(DocumentProcessingEvent event) {
        recordSafely(operationalEventFactory.from(event));
    }

    /**
     * Records the page view as operational analytics after its transaction commits.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(PageViewedEvent event) {
        recordSafely(operationalEventFactory.from(event));
    }

    /**
     * Attempts to save an operational event and logs the failure if analytics cannot be recorded.
     */
    private void recordSafely(OperationalEvent event) {
        try {
            operationalEventRecorder.record(event);
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to persist operational analytics event {} from source event {}",
                    event.getEventName(),
                    event.getSourceEventId(),
                    exception);
        }
    }
}