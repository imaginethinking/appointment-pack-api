package net.imaginethinking.appointmentpack.event.auth;

/**
 * Lists the supported values for authentication outcome.
 */
public enum AuthenticationOutcome {
    REQUESTED,
    RESENT,
    STARTED,
    CREATED,
    SUCCEEDED,
    FAILED,
    BLOCKED,
    ENABLED,
    DISABLED,
    MFA_REQUIRED
}