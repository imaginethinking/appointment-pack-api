package net.imaginethinking.appointmentpack.auth;

public record LoginResponse(
        boolean mfaRequired,
        String challengeToken,
        String accessToken,
        String tokenType
) {
}
