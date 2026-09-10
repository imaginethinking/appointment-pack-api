package net.imaginethinking.appointmentpack.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Defines the common ID and timestamp available on application events.
 */
public interface AppEvent {

    /**
     * Returns the unique ID carried by the event.
     */
    UUID eventId();

    /**
     * Returns when the event was created.
     */
    Instant occurredAt();
}