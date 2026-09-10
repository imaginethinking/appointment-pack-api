package net.imaginethinking.appointmentpack.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Checks that an authenticated JWT was issued for normal application access.
 */
@Component
public class AccessTokenPurposeValidator implements OAuth2TokenValidator<Jwt> {

    /**
     * Checks that the JWT contains the access purpose expected for normal signed in requests.
     */
    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String purpose = jwt.getClaimAsString(JwtClaims.PURPOSE);

        if (JwtClaims.ACCESS_PURPOSE.equals(purpose)) {
            return OAuth2TokenValidatorResult.success();
        }

        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "JWT purpose is invalid",
                null
        );

        return OAuth2TokenValidatorResult.failure(error);
    }
}