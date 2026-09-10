package net.imaginethinking.appointmentpack.auth;

import net.imaginethinking.appointmentpack.auth.mfa.*;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.security.JwtService;
import net.imaginethinking.appointmentpack.user.User;
import net.imaginethinking.appointmentpack.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Checks auth service behaviour across normal and failure cases.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MfaChallengeRepository mfaChallengeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private MfaTotpService mfaTotpService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private AppEventPublisher appEventPublisher;

    private AuthService authService;

    /**
     * Creates the common fixtures and mocks used by each test.
     */
    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                mfaChallengeRepository,
                passwordEncoder,
                jwtService,
                mfaTotpService,
                emailVerificationService,
                appEventPublisher);
    }

    @Test
    void shouldRegisterUnverifiedUserAndRequestVerification() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        RegisterRequest request = registerRequest();

        when(userRepository.existsByEmail("patient@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Appointment1!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", userId);
            ReflectionTestUtils.setField(user.getProfile(), "id", profileId);
            return user;
        });

        RegisterResponse response = authService.register(request);

        assertEquals(userId, response.id());
        assertEquals(profileId, response.profileId());
        assertEquals("patient@example.com", response.email());
        assertTrue(response.emailVerificationRequired());

        verify(emailVerificationService).issueInitialVerification(any(User.class));
        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldRejectDuplicateRegistrationEmail() {
        when(userRepository.existsByEmail("patient@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(registerRequest()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectMismatchedRegistrationPasswords() {
        RegisterRequest request = new RegisterRequest(
                "patient@example.com",
                "Appointment1!",
                "Different1!",
                "Patient",
                "User",
                LocalDate.of(1990, 1, 1));

        when(userRepository.existsByEmail("patient@example.com")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldRequireEmailVerificationBeforeLogin() {
        User user = user(false, false);

        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Appointment1!", "encoded-password")).thenReturn(true);

        LoginResponse response = authService.login(new LoginRequest("patient@example.com", "Appointment1!"));

        assertEquals(LoginStatus.EMAIL_VERIFICATION_REQUIRED, response.status());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void shouldAuthenticateVerifiedUserWithoutMfa() {
        User user = user(true, false);

        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Appointment1!", "encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("patient@example.com", "Appointment1!"));

        assertEquals(LoginStatus.AUTHENTICATED, response.status());
        assertEquals("jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
    }

    @Test
    void shouldCreateMfaChallengeForMfaEnabledUser() {
        User user = user(true, true);
        user.setMfaSecret("secret");
        UUID challengeId = UUID.randomUUID();

        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Appointment1!", "encoded-password")).thenReturn(true);
        when(mfaChallengeRepository.save(any(MfaChallenge.class))).thenAnswer(invocation -> {
            MfaChallenge challenge = invocation.getArgument(0);
            ReflectionTestUtils.setField(challenge, "id", challengeId);
            return challenge;
        });

        LoginResponse response = authService.login(new LoginRequest("patient@example.com", "Appointment1!"));

        assertEquals(LoginStatus.MFA_REQUIRED, response.status());
        assertEquals(challengeId, response.mfaChallengeId());
        verify(appEventPublisher, times(2)).publish(any());
    }

    @Test
    void shouldCompleteValidMfaLogin() {
        User user = user(true, true);
        user.setMfaSecret("secret");

        MfaChallenge challenge = new MfaChallenge();
        challenge.setUser(user);
        challenge.setExpiresAt(Instant.now().plusSeconds(300));
        UUID challengeId = UUID.randomUUID();
        ReflectionTestUtils.setField(challenge, "id", challengeId);

        when(mfaChallengeRepository.findByIdForUpdate(challengeId)).thenReturn(Optional.of(challenge));
        when(mfaTotpService.isValidCode("secret", "123456")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("jwt-token");

        LoginResponse response = authService.completeMfaLogin(new MfaLoginRequest(challengeId, "123456"));

        assertEquals(LoginStatus.AUTHENTICATED, response.status());
        assertEquals("jwt-token", response.accessToken());
        assertTrue(challenge.isUsed());
    }

    @Test
    void shouldConsumeChallengeAfterFifthInvalidMfaAttempt() {
        User user = user(true, true);
        user.setMfaSecret("secret");

        MfaChallenge challenge = new MfaChallenge();
        challenge.setUser(user);
        challenge.setExpiresAt(Instant.now().plusSeconds(300));
        challenge.setFailedAttempts(4);
        UUID challengeId = UUID.randomUUID();
        ReflectionTestUtils.setField(challenge, "id", challengeId);

        when(mfaChallengeRepository.findByIdForUpdate(challengeId)).thenReturn(Optional.of(challenge));
        when(mfaTotpService.isValidCode("secret", "000000")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.completeMfaLogin(new MfaLoginRequest(challengeId, "000000")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals(5, challenge.getFailedAttempts());
        assertTrue(challenge.isUsed());
    }

    @Test
    void shouldSetupAndConfirmMfa() {
        User user = user(true, false);
        UUID userId = user.getId();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mfaTotpService.generateSecret()).thenReturn("secret");
        when(mfaTotpService.generateProvisioningUri("patient@example.com", "secret")).thenReturn("otpauth://test");

        var setupResponse = authService.setupMfa(userId);

        assertEquals("otpauth://test", setupResponse.provisioningUri());
        assertEquals("secret", user.getMfaSecret());
        assertFalse(user.isMfaEnabled());

        when(mfaTotpService.isValidCode("secret", "123456")).thenReturn(true);

        authService.confirmMfa(userId, new MfaConfirmRequest("123456"));

        assertTrue(user.isMfaEnabled());
        verify(mfaTotpService).isValidCode(eq("secret"), eq("123456"));
    }

    /**
     * Creates test data for register request using the supplied values.
     */
    private RegisterRequest registerRequest() {
        return new RegisterRequest(
                " Patient@Example.com ",
                "Appointment1!",
                "Appointment1!",
                " Patient ",
                " User ",
                LocalDate.of(1990, 1, 1));
    }

    /**
     * Creates a test user with the supplied values.
     */
    private User user(boolean verified, boolean mfaEnabled) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        user.setEmail("patient@example.com");
        user.setPasswordHash("encoded-password");
        user.setEnabled(true);
        user.setMfaEnabled(mfaEnabled);

        if (verified) {
            user.setEmailVerifiedAt(Instant.now());
        }

        return user;
    }
}