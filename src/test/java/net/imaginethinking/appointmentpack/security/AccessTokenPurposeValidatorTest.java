package net.imaginethinking.appointmentpack.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessTokenPurposeValidatorTest {

    private final AccessTokenPurposeValidator validator = new AccessTokenPurposeValidator();

    @Test
    void shouldAcceptAccessTokenPurpose() {
        Jwt jwt = jwtWithPurpose(JwtClaims.ACCESS_PURPOSE);

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertFalse(result.hasErrors());
    }

    @Test
    void shouldRejectMissingOrIncorrectPurpose() {
        OAuth2TokenValidatorResult incorrect = validator.validate(jwtWithPurpose("PASSWORD_RESET"));

        OAuth2TokenValidatorResult missing = validator.validate(jwtWithPurpose(null));

        assertTrue(incorrect.hasErrors());
        assertTrue(missing.hasErrors());
    }

    private Jwt jwtWithPurpose(String purpose) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));

        if (purpose != null) {
            builder.claim(JwtClaims.PURPOSE, purpose);
        }

        return builder.build();
    }
}