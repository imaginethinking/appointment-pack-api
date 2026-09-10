package net.imaginethinking.appointmentpack.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Reads the user ID stored in the subject of an authenticated JWT.
 */
@Component
public class AuthenticatedUserIdResolver {

    /**
     * Reads the user ID from the JWT subject and rejects tokens that do not contain a valid UUID.
     */
    public UUID resolve(Jwt jwt) {
        String subject = jwt.getSubject();

        if (subject == null) {
            throw invalidAccessToken();
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw invalidAccessToken();
        }
    }

    /**
     * Creates the unauthorized response used when the access token does not contain a valid user ID.
     */
    private ResponseStatusException invalidAccessToken() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid access token"
        );
    }

}
