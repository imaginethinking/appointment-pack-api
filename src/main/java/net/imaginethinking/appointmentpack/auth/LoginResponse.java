package net.imaginethinking.appointmentpack.auth;

import java.util.UUID;

/**
 * Represents login information returned by the API.
 */
public record LoginResponse(
        LoginStatus status,
        UUID mfaChallengeId,
        String accessToken,
        String tokenType
) {

    /**
     * Creates the successful login response containing the access token.
     */
    public static LoginResponse authenticated(String accessToken) {
        return new LoginResponse(
                LoginStatus.AUTHENTICATED,
                null,
                accessToken,
                "Bearer"
        );
    }

    /**
     * Creates the login response used when the account still needs email verification.
     */
    public static LoginResponse pendingEmailVerification() {
        return new LoginResponse(
                LoginStatus.EMAIL_VERIFICATION_REQUIRED,
                null,
                null,
                null
        );
    }

    /**
     * Creates the login response containing the MFA challenge that must be completed next.
     */
    public static LoginResponse pendingMfa(UUID mfaChallengeId) {
        return new LoginResponse(
                LoginStatus.MFA_REQUIRED,
                mfaChallengeId,
                null,
                null
        );
    }
}