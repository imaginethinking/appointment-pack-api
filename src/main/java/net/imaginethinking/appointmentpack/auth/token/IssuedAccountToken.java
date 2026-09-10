package net.imaginethinking.appointmentpack.auth.token;

import java.time.Instant;

/**
 * Keeps the raw account token and expiry returned immediately after a new token is issued.
 */
public record IssuedAccountToken(
        String token,
        Instant expiresAt
) {
}