package net.imaginethinking.appointmentpack.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
public class AuthenticatedUserIdResolver {

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

    private ResponseStatusException invalidAccessToken() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid access token"
        );
    }

}
