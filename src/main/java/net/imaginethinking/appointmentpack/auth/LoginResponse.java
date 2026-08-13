package net.imaginethinking.appointmentpack.auth;

import java.util.UUID;

public record LoginResponse(
        LoginStatus status,
        UUID mfaChallengeId,
        String accessToken,
        String tokenType
) {

    public static LoginResponse authenticated(String accessToken) {
        return new LoginResponse(
                LoginStatus.AUTHENTICATED,
                null,
                accessToken,
                "Bearer"
        );
    }

    public static LoginResponse pendingEmailVerification() {
        return new LoginResponse(
                LoginStatus.EMAIL_VERIFICATION_REQUIRED,
                null,
                null,
                null
        );
    }

    public static LoginResponse pendingMfa(UUID mfaChallengeId) {
        return new LoginResponse(
                LoginStatus.MFA_REQUIRED,
                mfaChallengeId,
                null,
                null
        );
    }
}