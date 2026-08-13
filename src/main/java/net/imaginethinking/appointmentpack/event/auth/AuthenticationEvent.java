package net.imaginethinking.appointmentpack.event.auth;

import net.imaginethinking.appointmentpack.event.AppEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuthenticationEvent(
        UUID eventId,
        Instant occurredAt,
        UUID actorUserId,
        AuthenticationAction action,
        AuthenticationOutcome outcome
) implements AppEvent {

    public AuthenticationEvent {
        Objects.requireNonNull(eventId, "Event ID must not be null");
        Objects.requireNonNull(occurredAt, "Event timestamp must not be null");
        Objects.requireNonNull(action, "Authentication action must not be null");
        Objects.requireNonNull(outcome, "Authentication outcome must not be null");
    }

    public static AuthenticationEvent create(
            UUID actorUserId,
            AuthenticationAction action,
            AuthenticationOutcome outcome
    ) {
        return new AuthenticationEvent(
                UUID.randomUUID(),
                Instant.now(),
                actorUserId,
                action,
                outcome
        );
    }
}