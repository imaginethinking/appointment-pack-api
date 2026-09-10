package net.imaginethinking.appointmentpack.event.notification;

import net.imaginethinking.appointmentpack.event.AppEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Carries the values needed to send a password reset email without exposing them in its string output.
 */
public final class PasswordResetEmailRequestedEvent implements AppEvent {

    private final UUID eventId;
    private final Instant occurredAt;
    private final UUID userId;
    private final String recipientEmail;
    private final String token;
    private final Instant expiresAt;

    /**
     * Checks the password reset email values before storing the immutable event details.
     */
    private PasswordResetEmailRequestedEvent(
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
        this.token = Objects.requireNonNull(token, "Password reset token must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Token expiry must not be null");
    }

    /**
     * Creates a new password reset email requested event from the submitted values.
     */
    public static PasswordResetEmailRequestedEvent create(
            UUID userId,
            String recipientEmail,
            String token,
            Instant expiresAt) {
        return new PasswordResetEmailRequestedEvent(
                UUID.randomUUID(),
                Instant.now(),
                userId,
                recipientEmail,
                token,
                expiresAt);
    }

    /**
     * Returns the unique ID carried by the event.
     */
    @Override
    public UUID eventId() {
        return eventId;
    }

    /**
     * Returns when the event was created.
     */
    @Override
    public Instant occurredAt() {
        return occurredAt;
    }

    /**
     * Returns the user ID carried by the event.
     */
    public UUID userId() {
        return userId;
    }

    /**
     * Returns the email address that should receive the notification.
     */
    public String recipientEmail() {
        return recipientEmail;
    }

    /**
     * Returns the account token included in the notification request.
     */
    public String token() {
        return token;
    }

    /**
     * Returns when the account token expires.
     */
    public Instant expiresAt() {
        return expiresAt;
    }

    /**
     * Returns a safe event description that leaves the reset token out of log output.
     */
    @Override
    public String toString() {
        return "PasswordResetEmailRequestedEvent["
                + "eventId=" + eventId
                + ", occurredAt=" + occurredAt
                + ", userId=" + userId
                + ", expiresAt=" + expiresAt
                + ']';
    }
}