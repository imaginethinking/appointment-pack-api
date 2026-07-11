package net.imaginethinking.appointmentpack.auth;

public record AuthResponse(
        boolean mfaRequired,
        String challengeId,
        String accessToken,
        String tokenType
) {
}
