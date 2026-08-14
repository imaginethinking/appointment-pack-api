package net.imaginethinking.appointmentpack.security;

import lombok.AllArgsConstructor;
import net.imaginethinking.appointmentpack.user.User;
import net.imaginethinking.appointmentpack.user.UserRole;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@AllArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;

    public String generateAccessToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("appointment-pack-api")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .subject(user.getId().toString())
                .claim("purpose", "ACCESS")
                .claim("roles", rolesFor(user))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }

    private List<String> rolesFor(User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return List.of(
                    UserRole.USER.name(),
                    UserRole.ADMIN.name()
            );
        }

        return List.of(UserRole.USER.name());
    }
}