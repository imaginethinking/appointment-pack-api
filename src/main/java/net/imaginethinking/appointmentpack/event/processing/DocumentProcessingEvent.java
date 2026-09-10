package net.imaginethinking.appointmentpack.event.processing;

import net.imaginethinking.appointmentpack.document.DocumentType;
import net.imaginethinking.appointmentpack.event.AppEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Records the outcome, duration and failure reason for one document processing operation.
 */
public record DocumentProcessingEvent(
        UUID eventId,
        Instant occurredAt,
        UUID actorUserId,
        UUID documentId,
        DocumentType documentType,
        DocumentProcessingOperation operation,
        ProcessingOutcome outcome,
        Long durationMs,
        DocumentProcessingFailureReason failureReason,
        String processorVersion,
        String modelName,
        String promptVersion
) implements AppEvent {

    /**
     * Checks the processing event values and keeps the recorded event unchanged after creation.
     */
    public DocumentProcessingEvent {
        Objects.requireNonNull(eventId, "Event ID must not be null");
        Objects.requireNonNull(occurredAt, "Event timestamp must not be null");
        Objects.requireNonNull(actorUserId, "Actor user ID must not be null");
        Objects.requireNonNull(documentId, "Document ID must not be null");
        Objects.requireNonNull(documentType, "Document type must not be null");
        Objects.requireNonNull(operation, "Processing operation must not be null");
        Objects.requireNonNull(outcome, "Processing outcome must not be null");

        if (durationMs != null && durationMs < 0) {
            throw new IllegalArgumentException("Processing duration must not be negative");
        }

        if (outcome == ProcessingOutcome.SUCCEEDED
                && failureReason != null) {
            throw new IllegalArgumentException("Successful processing events must not have a failure reason");
        }
    }

    /**
     * Creates a successful processing event with the operation, duration and document details.
     */
    public static DocumentProcessingEvent succeeded(
            UUID actorUserId,
            UUID documentId,
            DocumentType documentType,
            DocumentProcessingOperation operation,
            Long durationMs,
            String processorVersion,
            String modelName,
            String promptVersion
    ) {
        return new DocumentProcessingEvent(
                UUID.randomUUID(),
                Instant.now(),
                actorUserId,
                documentId,
                documentType,
                operation,
                ProcessingOutcome.SUCCEEDED,
                durationMs,
                null,
                processorVersion,
                modelName,
                promptVersion
        );
    }

    /**
     * Creates a failed processing event with the operation, duration and failure reason.
     */
    public static DocumentProcessingEvent failed(
            UUID actorUserId,
            UUID documentId,
            DocumentType documentType,
            DocumentProcessingOperation operation,
            Long durationMs,
            DocumentProcessingFailureReason failureReason
    ) {
        return new DocumentProcessingEvent(
                UUID.randomUUID(),
                Instant.now(),
                actorUserId,
                documentId,
                documentType,
                operation,
                ProcessingOutcome.FAILED,
                durationMs,
                Objects.requireNonNull(
                        failureReason,
                        "Failure reason must not be null"
                ),
                null,
                null,
                null
        );
    }
}