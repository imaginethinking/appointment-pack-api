package net.imaginethinking.appointmentpack.event.auth;

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