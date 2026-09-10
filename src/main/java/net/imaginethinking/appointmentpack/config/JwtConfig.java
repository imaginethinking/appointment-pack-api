package net.imaginethinking.appointmentpack.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.annotation.PostConstruct;
import net.imaginethinking.appointmentpack.security.AccessTokenPurposeValidator;
import net.imaginethinking.appointmentpack.security.JwtClaims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Creates the JWT encoder and decoder from the configured signing secret.
 */
@Configuration
public class JwtConfig {
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /**
     * Checks that the configured JWT secret is present and long enough for HS256 signing.
     */
    @PostConstruct
    void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("Missing required property: app.jwt.secret");
        }

        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("Property app.jwt.secret must be at least 32 bytes for HS256");
        }
    }

    /**
     * Creates the JWT encoder using the configured HMAC signing key.
     */
    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecret.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Creates the JWT decoder and applies the expected issuer and access purpose checks.
     */
    @Bean
    public JwtDecoder jwtDecoder(AccessTokenPurposeValidator accessTokenPurposeValidator) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(getSigningKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(
                        JwtClaims.ISSUER), accessTokenPurposeValidator);

        jwtDecoder.setJwtValidator(validator);

        return jwtDecoder;
    }

    /**
     * Builds the HMAC signing key from the configured JWT secret.
     */
    private SecretKey getSigningKey() {
        return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}