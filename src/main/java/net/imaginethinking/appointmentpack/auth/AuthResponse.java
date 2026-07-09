package net.imaginethinking.appointmentpack.auth;

public record AuthResponse(
        String accessToken,
        String tokenType
) {
}
