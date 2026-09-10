package net.imaginethinking.appointmentpack.auth;

/**
 * Lists the supported values for login status.
 */
public enum LoginStatus {
    AUTHENTICATED,
    EMAIL_VERIFICATION_REQUIRED,
    MFA_REQUIRED
}