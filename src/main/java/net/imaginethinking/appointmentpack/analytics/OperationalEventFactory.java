package net.imaginethinking.appointmentpack.analytics;

import net.imaginethinking.appointmentpack.event.analytics.PageViewedEvent;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationEvent;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingEvent;
import org.springframework.stereotype.Component;

@Component
public class OperationalEventFactory {

    public OperationalEvent from(PatientActivityEvent event) {
        return new OperationalEvent(
                event.eventId(),
                OperationalEventCategory.PATIENT_ACTIVITY,
                event.resourceType().name() + "_" + event.action().name(),
                event.actorUserId(),
                event.occurredAt(),
                event.resourceType(),
                event.action(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public OperationalEvent from(AuthenticationEvent event) {
        return new OperationalEvent(
                event.eventId(),
                OperationalEventCategory.AUTHENTICATION,
                event.action().name() + "_" + event.outcome().name(),
                event.userId(),
                event.occurredAt(),
                null,
                null,
                event.action(),
                event.outcome(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public OperationalEvent from(DocumentProcessingEvent event) {
        return new OperationalEvent(
                event.eventId(),
                OperationalEventCategory.DOCUMENT_PROCESSING,
                event.operation().name() + "_" + event.outcome().name(),
                event.actorUserId(),
                event.occurredAt(),
                null,
                null,
                null,
                null,
                event.documentType(),
                event.operation(),
                event.outcome(),
                event.failureReason(),
                event.durationMs(),
                event.processorVersion(),
                event.modelName(),
                event.promptVersion(),
                null
        );
    }

    public OperationalEvent from(PageViewedEvent event) {
        return new OperationalEvent(
                event.eventId(),
                OperationalEventCategory.PAGE_VIEW,
                "PAGE_VIEWED",
                event.actorUserId(),
                event.occurredAt(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                event.page()
        );
    }
}