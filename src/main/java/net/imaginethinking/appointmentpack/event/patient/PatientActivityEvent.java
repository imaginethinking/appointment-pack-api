package net.imaginethinking.appointmentpack.event.patient;

import net.imaginethinking.appointmentpack.event.AppEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Records a significant action performed against a patient resource.
 */
public record PatientActivityEvent(
        UUID eventId,
        Instant occurredAt,
        UUID actorUserId,
        UUID patientRecordId,
        PatientResourceType resourceType,
        UUID resourceId,
        PatientActivityAction action
) implements AppEvent {

    /**
     * Checks the patient activity values and keeps the event unchanged after creation.
     */
    public PatientActivityEvent {
        Objects.requireNonNull(eventId, "Event ID must not be null");
        Objects.requireNonNull(occurredAt, "Event timestamp must not be null");
        Objects.requireNonNull(actorUserId, "Actor user ID must not be null");
        Objects.requireNonNull(patientRecordId, "Patient record ID must not be null");
        Objects.requireNonNull(resourceType, "Resource type must not be null");
        Objects.requireNonNull(resourceId, "Resource ID must not be null");
        Objects.requireNonNull(action, "Action must not be null");
    }

    /**
     * Creates a new patient activity event from the submitted values.
     */
    public static PatientActivityEvent create(
            UUID actorUserId,
            UUID patientRecordId,
            PatientResourceType resourceType,
            UUID resourceId,
            PatientActivityAction action
    ) {
        return new PatientActivityEvent(
                UUID.randomUUID(),
                Instant.now(),
                actorUserId,
                patientRecordId,
                resourceType,
                resourceId,
                action
        );
    }
}