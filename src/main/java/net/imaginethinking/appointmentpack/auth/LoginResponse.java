package net.imaginethinking.appointmentpack.auth;

import java.util.UUID;

public record LoginResponse(
        boolean mfaRequired,
        UUID mfaChallengeId,
        String accessToken,
        String tokenType
) {
    
    public static LoginResponse authenticated(String accessToken) {
        return new LoginResponse(
                false,
                null,
                accessToken,
                "Bearer"
        );
    }

    public static LoginResponse pendingMfa(UUID mfaChallengeId) {
        return new LoginResponse(
                true,
                mfaChallengeId,
                null,
                null
        );
    }

}
