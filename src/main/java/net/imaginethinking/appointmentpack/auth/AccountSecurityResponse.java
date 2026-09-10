package net.imaginethinking.appointmentpack.auth;

/**
 * Represents account security information returned by the API.
 */
public record AccountSecurityResponse(
        boolean mfaEnabled
) {
}
