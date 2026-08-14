package net.imaginethinking.appointmentpack.analytics.admin;

import net.imaginethinking.appointmentpack.analytics.OperationalEvent;
import net.imaginethinking.appointmentpack.analytics.OperationalEventCategory;
import net.imaginethinking.appointmentpack.document.DocumentType;
import net.imaginethinking.appointmentpack.event.analytics.ApplicationPage;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationAction;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationOutcome;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingFailureReason;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingOperation;
import net.imaginethinking.appointmentpack.event.processing.ProcessingOutcome;

import java.time.Instant;
import java.util.UUID;

public record OperationalEventResponse(
        UUID id,
        OperationalEventCategory category,
        String eventName,
        UUID userId,
        Instant occurredAt,
        PatientResourceType patientResourceType,
        PatientActivityAction patientAction,
        AuthenticationAction authenticationAction,
        AuthenticationOutcome authenticationOutcome,
        DocumentType documentType,
        DocumentProcessingOperation processingOperation,
        ProcessingOutcome processingOutcome,
        DocumentProcessingFailureReason processingFailureReason,
        Long durationMs,
        String processorVersion,
        String modelName,
        String promptVersion,
        ApplicationPage page
) {

    public static OperationalEventResponse from(OperationalEvent event) {
        return new OperationalEventResponse(
                event.getId(),
                event.getCategory(),
                event.getEventName(),
                event.getUserId(),
                event.getOccurredAt(),
                event.getPatientResourceType(),
                event.getPatientAction(),
                event.getAuthenticationAction(),
                event.getAuthenticationOutcome(),
                event.getDocumentType(),
                event.getProcessingOperation(),
                event.getProcessingOutcome(),
                event.getProcessingFailureReason(),
                event.getDurationMs(),
                event.getProcessorVersion(),
                event.getModelName(),
                event.getPromptVersion(),
                event.getPage()
        );
    }
}