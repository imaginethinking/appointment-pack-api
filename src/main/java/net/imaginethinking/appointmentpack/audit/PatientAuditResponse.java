package net.imaginethinking.appointmentpack.audit;

import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import net.imaginethinking.appointmentpack.user.User;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents patient activity information returned by the API.
 */
public record PatientAuditResponse(
        UUID id,
        UUID actorUserId,
        String actorDisplayName,
        PatientResourceType resourceType,
        UUID resourceId,
        PatientActivityAction action,
        Instant occurredAt
) {

    /**
     * Builds an activity response from the saved audit event and the user who performed it.
     */
    public static PatientAuditResponse from(
            PatientAuditEvent auditEvent,
            User actor
    ) {
        return new PatientAuditResponse(
                auditEvent.getId(),
                auditEvent.getActorUserId(),
                actorDisplayName(actor),
                auditEvent.getResourceType(),
                auditEvent.getResourceId(),
                auditEvent.getAction(),
                auditEvent.getOccurredAt()
        );
    }

    /**
     * Builds the actor name from the stored profile and falls back to the account email when needed.
     */
    private static String actorDisplayName(User actor) {
        if (actor == null || actor.getProfile() == null) {
            return null;
        }

        return actor.getProfile().getFirstName() + " " + actor.getProfile().getLastName();
    }
}