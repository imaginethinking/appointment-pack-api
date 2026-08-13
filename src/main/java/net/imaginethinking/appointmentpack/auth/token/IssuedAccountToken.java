package net.imaginethinking.appointmentpack.auth.token;

import java.time.Instant;

public record IssuedAccountToken(
        String token,
        Instant expiresAt
) {
}