package net.imaginethinking.appointmentpack.auth;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.auth.mfa.*;
import net.imaginethinking.appointmentpack.common.TextNormalizer;
import net.imaginethinking.appointmentpack.profile.Profile;
import net.imaginethinking.appointmentpack.security.JwtService;
import net.imaginethinking.appointmentpack.auth.mfa.MfaTotpService;
import net.imaginethinking.appointmentpack.user.EmailAddressNormalizer;
import net.imaginethinking.appointmentpack.user.User;
import net.imaginethinking.appointmentpack.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration MFA_CHALLENGE_LIFETIME = Duration.ofMinutes(5);
    private static final int MAX_MFA_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final MfaChallengeRepository mfaChallengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MfaTotpService mfaTotpService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = EmailAddressNormalizer.normalise(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email address already registered"
            );
        }

        if (!Objects.equals(request.password(), request.confirmPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passwords do not match"
            );
        }

        User user = new User();

        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        Profile profile = new Profile();
        profile.setUser(user);
        profile.setFirstName(TextNormalizer.strip(request.firstName()));
        profile.setLastName(TextNormalizer.strip(request.lastName()));
        profile.setDateOfBirth(request.dateOfBirth());

        user.setProfile(profile);

        User registeredUser = userRepository.save(user);

        return new RegisterResponse(
                registeredUser.getId(),
                registeredUser.getEmail(),
                registeredUser.getProfile().getId());
    }

    public LoginResponse login(LoginRequest request) {
        String email = EmailAddressNormalizer.normalise(request.email());

        User user = userRepository.findByEmail(email).orElseThrow(this::invalidCredentials);

        boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());

        if (!passwordMatches || !user.isEnabled()) {
            throw invalidCredentials();
        }

        if (!user.isMfaEnabled()) {
            String accessToken = jwtService.generateAccessToken(user);

            return LoginResponse.authenticated(accessToken);
        }

        if (user.getMfaSecret() == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "MFA configuration is invalid"
            );
        }

        MfaChallenge challenge = new MfaChallenge();
        challenge.setUser(user);
        challenge.setExpiresAt(Instant.now().plus(MFA_CHALLENGE_LIFETIME));

        MfaChallenge savedChallenge = mfaChallengeRepository.save(challenge);

        return LoginResponse.pendingMfa(savedChallenge.getId());
    }

    @Transactional
    public MfaSetupResponse setupMfa(UUID userId) {
        User user = findUser(userId);

        if (user.isMfaEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MFA is already enabled");
        }

        String secret = mfaTotpService.generateSecret();
        String provisioningUri = mfaTotpService.generateProvisioningUri(user.getEmail(), secret);

        user.setMfaSecret(secret);
        user.setMfaEnabled(false);

        return new MfaSetupResponse(provisioningUri);
    }

    @Transactional
    public void confirmMfa(UUID userId, MfaConfirmRequest request) {
        User user = findUser(userId);

        if (user.isMfaEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "MFA is already enabled"
            );
        }

        if (user.getMfaSecret() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "MFA setup has not been started"
            );
        }

        boolean valid = mfaTotpService.isValidCode(user.getMfaSecret(), request.code());

        if (!valid) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid MFA code"
            );
        }

        user.setMfaEnabled(true);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public LoginResponse completeMfaLogin(MfaLoginRequest request) {
        MfaChallenge challenge = mfaChallengeRepository.findById(request.mfaChallengeId())
                .orElseThrow(this::invalidMfaChallenge);

        Instant now = Instant.now();

        if (challenge.isUsed() || !challenge.getExpiresAt()
                .isAfter(now) || challenge.getFailedAttempts() >= MAX_MFA_ATTEMPTS) {
            throw invalidMfaChallenge();
        }

        User user = challenge.getUser();

        if (!user.isEnabled() || !user.isMfaEnabled() || user.getMfaSecret() == null) {
            challenge.setUsed(true);
            throw invalidMfaChallenge();
        }

        boolean valid = mfaTotpService.isValidCode(user.getMfaSecret(), request.code());

        if (!valid) {
            int attempts = challenge.getFailedAttempts() + 1;
            challenge.setFailedAttempts(attempts);

            if (attempts >= MAX_MFA_ATTEMPTS) {
                challenge.setUsed(true);
            }

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid MFA code"
            );
        }

        challenge.setUsed(true);

        String accessToken = jwtService.generateAccessToken(user);

        return LoginResponse.authenticated(accessToken);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found")
                );
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
        );
    }

    private ResponseStatusException invalidMfaChallenge() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid or expired MFA challenge"
        );
    }
}