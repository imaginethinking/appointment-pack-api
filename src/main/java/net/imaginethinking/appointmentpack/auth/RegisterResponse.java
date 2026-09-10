package net.imaginethinking.appointmentpack.auth;

import java.util.UUID;

/**
 * Represents register information returned by the API.
 */
public record RegisterResponse(
        UUID id,
        String email,
        UUID profileId,
        boolean emailVerificationRequired
) {
}