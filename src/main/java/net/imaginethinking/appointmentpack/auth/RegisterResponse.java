package net.imaginethinking.appointmentpack.auth;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String email,
        UUID profileId,
        boolean emailVerificationRequired
) {
}
