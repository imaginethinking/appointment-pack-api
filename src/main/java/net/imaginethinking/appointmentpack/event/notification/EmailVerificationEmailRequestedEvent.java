package net.imaginethinking.appointmentpack.event.notification;

import net.imaginethinking.appointmentpack.event.AppEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class EmailVerificationEmailRequestedEvent implements AppEvent {

    private final UUID eventId;
    private final Instant occurredAt;
    private final UUID userId;
    private final String recipientEmail;
    private final String token;
    private final Instant expiresAt;

    private EmailVerificationEmailRequestedEvent(
            UUID eventId,
            Instant occurredAt,
            UUID userId,
            String recipientEmail,
            String token,
            Instant expiresAt) {
        this.eventId = Objects.requireNonNull(eventId, "Event ID must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Event timestamp must not be null");
        this.userId = Objects.requireNonNull(userId, "User ID must not be null");
        this.recipientEmail = Objects.requireNonNull(recipientEmail, "Recipient email must not be null");
        this.token = Objects.requireNonNull(token, "Verification token must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Token expiry must not be null");
    }

    public static EmailVerificationEmailRequestedEvent create(
            UUID userId,
            String recipientEmail,
            String token,
            Instant expiresAt) {
        return new EmailVerificationEmailRequestedEvent(
                UUID.randomUUID(),
                Instant.now(),
                userId,
                recipientEmail,
                token,
                expiresAt);
    }

    @Override
    public UUID eventId() {
        return eventId;
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }

    public UUID userId() {
        return userId;
    }

    public String recipientEmail() {
        return recipientEmail;
    }

    public String token() {
        return token;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        return "EmailVerificationEmailRequestedEvent["
                + "eventId=" + eventId
                + ", occurredAt=" + occurredAt
                + ", userId=" + userId
                + ", expiresAt=" + expiresAt
                + ']';
    }
}