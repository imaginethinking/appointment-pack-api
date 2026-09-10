package net.imaginethinking.appointmentpack.auth.mfa;

/**
 * Represents MFA setup information returned by the API.
 */
public record MfaSetupResponse(
        String provisioningUri
) {
}
