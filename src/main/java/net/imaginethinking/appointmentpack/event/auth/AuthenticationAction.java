package net.imaginethinking.appointmentpack.event.auth;

public enum AuthenticationAction {
    REGISTRATION,
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
    PASSWORD_CHANGE,
    LOGIN,
    MFA_SETUP,
    MFA_DISABLE,
    MFA_CHALLENGE,
    MFA_LOGIN
}