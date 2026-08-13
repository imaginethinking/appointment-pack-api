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

@Slf4j
@Component
@RequiredArgsConstructor
public class OperationalEventListener {

    private final OperationalEventFactory operationalEventFactory;
    private final OperationalEventRecorder operationalEventRecorder;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(PatientActivityEvent event) {
        recordSafely(operationalEventFactory.from(event));
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(AuthenticationEvent event) {
        recordSafely(operationalEventFactory.from(event));
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(DocumentProcessingEvent event) {
        recordSafely(operationalEventFactory.from(event));
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(PageViewedEvent event) {
        recordSafely(operationalEventFactory.from(event));
    }

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