package net.imaginethinking.appointmentpack.event;

import java.time.Instant;
import java.util.UUID;

public interface AppEvent {

    UUID eventId();

    Instant occurredAt();
}