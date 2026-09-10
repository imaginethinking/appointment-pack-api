package net.imaginethinking.appointmentpack.event.analytics;

import net.imaginethinking.appointmentpack.event.AppEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Records ab application page view for operational analytics.
 */
public record PageViewedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID actorUserId,
        ApplicationPage page
) implements AppEvent {

    /**
     * Checks the page view values and keeps the event unchanged after creation.
     */
    public PageViewedEvent {
        Objects.requireNonNull(eventId, "Event ID must not be null");
        Objects.requireNonNull(occurredAt, "Event timestamp must not be null");
        Objects.requireNonNull(page, "Application page must not be null");
    }

    /**
     * Creates a new page viewed event from the submitted values.
     */
    public static PageViewedEvent create(
            UUID actorUserId,
            ApplicationPage page
    ) {
        return new PageViewedEvent(
                UUID.randomUUID(),
                Instant.now(),
                actorUserId,
                page
        );
    }
}