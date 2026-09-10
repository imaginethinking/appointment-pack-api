package net.imaginethinking.appointmentpack.security;

import net.imaginethinking.appointmentpack.user.User;
import net.imaginethinking.appointmentpack.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Checks JWT service behaviour across normal and failure cases.
 */
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    private JwtService jwtService;

    /**
     * Creates the common fixtures and mocks used by each test.
     */
    @BeforeEach
    void setUp() {
        jwtService = new JwtService(jwtEncoder);

        Jwt encodedJwt = org.mockito.Mockito.mock(Jwt.class);

        when(encodedJwt.getTokenValue()).thenReturn("encoded-token");

        when(jwtEncoder.encode(any())).thenReturn(encodedJwt);
    }

    @Test
    void shouldGenerateAccessTokenForNormalUser() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, UserRole.USER);

        String token = jwtService.generateAccessToken(user);

        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);

        org.mockito.Mockito.verify(jwtEncoder).encode(captor.capture());

        Map<String, Object> claims = captor.getValue().getClaims().getClaims();

        assertEquals("encoded-token", token);
        assertEquals(JwtClaims.ISSUER, claims.get("iss"));
        assertEquals(userId.toString(), claims.get("sub"));
        assertEquals(JwtClaims.ACCESS_PURPOSE, claims.get(JwtClaims.PURPOSE));
        assertEquals(List.of("USER"), claims.get(JwtClaims.ROLES));
    }

    @Test
    void shouldIncludeAdminRoleForAdministrator() {
        User admin = user(UUID.randomUUID(), UserRole.ADMIN);

        jwtService.generateAccessToken(admin);

        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);

        org.mockito.Mockito.verify(jwtEncoder).encode(captor.capture());

        Map<String, Object> claims = captor.getValue().getClaims().getClaims();

        assertEquals(List.of("USER", "ADMIN"), claims.get(JwtClaims.ROLES));
    }

    /**
     * Creates a test user with the supplied values.
     */
    private User user(UUID id, UserRole role) {
        User user = new User();

        ReflectionTestUtils.setField(user, "id", id);

        user.setRole(role);

        return user;
    }
}