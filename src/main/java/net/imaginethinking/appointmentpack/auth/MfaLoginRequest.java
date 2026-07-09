package net.imaginethinking.appointmentpack.auth;

public record MfaLoginRequest(
        String challengeToken,
        String code
) {
}
