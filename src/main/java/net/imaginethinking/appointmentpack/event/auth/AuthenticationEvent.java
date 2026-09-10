package net.imaginethinking.appointmentpack.event.auth;

import net.imaginethinking.appointmentpack.event.AppEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Records an authentication action and its outcome for later audit or analytics handling.
 */
public record AuthenticationEvent(
        UUID eventId,
        Instant occurredAt,
        UUID userId,
        AuthenticationAction action,
        AuthenticationOutcome outcome
) implements AppEvent {

    /**
     * Checks the event values and keeps the authentication event unchanged after creation.
     */
    public AuthenticationEvent {
        Objects.requireNonNull(eventId, "Event ID must not be null");
        Objects.requireNonNull(occurredAt, "Event timestamp must not be null");
        Objects.requireNonNull(action, "Authentication action must not be null");
        Objects.requireNonNull(outcome, "Authentication outcome must not be null");
    }

    /**
     * Creates a new authentication event from the submitted values.
     */
    public static AuthenticationEvent create(UUID userId, AuthenticationAction action, AuthenticationOutcome outcome) {
        return new AuthenticationEvent(UUID.randomUUID(), Instant.now(), userId, action, outcome);
    }
}