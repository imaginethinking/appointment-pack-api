package net.imaginethinking.appointmentpack.event.auth;

public enum AuthenticationAction {
    REGISTRATION,
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
    LOGIN,
    MFA_SETUP,
    MFA_CHALLENGE,
    MFA_LOGIN
}