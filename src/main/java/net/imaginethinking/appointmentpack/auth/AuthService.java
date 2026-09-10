package net.imaginethinking.appointmentpack.auth;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.auth.mfa.*;
import net.imaginethinking.appointmentpack.common.TextNormalizer;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationAction;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationEvent;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationOutcome;
import net.imaginethinking.appointmentpack.profile.Profile;
import net.imaginethinking.appointmentpack.security.JwtService;
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

/**
 * Handles account registration, login and MFA while recording the main authentication outcomes.
 */
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
    private final EmailVerificationService emailVerificationService;
    private final AppEventPublisher appEventPublisher;

    /**
     * Normalises the email, checks the submitted passwords, creates the account and profile, then starts email
     * verification.
     */
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

        emailVerificationService.issueInitialVerification(registeredUser);

        appEventPublisher.publish(AuthenticationEvent.create(
                registeredUser.getId(),
                AuthenticationAction.REGISTRATION,
                AuthenticationOutcome.SUCCEEDED));

        return new RegisterResponse(
                registeredUser.getId(),
                registeredUser.getEmail(),
                registeredUser.getProfile().getId(),
                true);
    }

    /**
     * Checks the email and password, handles verification and MFA requirements, then returns an access token when
     * login can complete.
     *
     * @param request email and password entered for the login attempt
     * @return the next login state or an authenticated access token
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public LoginResponse login(LoginRequest request) {
        String email = EmailAddressNormalizer.normalise(request.email());

        User user = userRepository.findByEmail(email).orElse(null);

        // Use the same login error for unknown users and incorrect passwords so the response does not reveal
        // whether an account exists.
        if (user == null) {
            publishAuthenticationEvent(null, AuthenticationAction.LOGIN, AuthenticationOutcome.FAILED);

            throw invalidCredentials();
        }

        boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());

        if (!passwordMatches || !user.isEnabled()) {
            publishAuthenticationEvent(user.getId(), AuthenticationAction.LOGIN, AuthenticationOutcome.FAILED);

            throw invalidCredentials();
        }

        if (!user.isEmailVerified()) {
            publishAuthenticationEvent(user.getId(), AuthenticationAction.LOGIN, AuthenticationOutcome.BLOCKED);

            return LoginResponse.pendingEmailVerification();
        }

        if (!user.isMfaEnabled()) {
            String accessToken = jwtService.generateAccessToken(user);

            publishAuthenticationEvent(user.getId(), AuthenticationAction.LOGIN, AuthenticationOutcome.SUCCEEDED);

            return LoginResponse.authenticated(accessToken);
        }

        if (user.getMfaSecret() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "MFA configuration is invalid");
        }

        MfaChallenge challenge = new MfaChallenge();
        challenge.setUser(user);
        challenge.setExpiresAt(Instant.now().plus(MFA_CHALLENGE_LIFETIME));

        MfaChallenge savedChallenge = mfaChallengeRepository.save(challenge);

        publishAuthenticationEvent(user.getId(), AuthenticationAction.LOGIN, AuthenticationOutcome.MFA_REQUIRED);
        publishAuthenticationEvent(user.getId(), AuthenticationAction.MFA_CHALLENGE, AuthenticationOutcome.CREATED);

        return LoginResponse.pendingMfa(savedChallenge.getId());
    }

    /**
     * Loads the user and returns whether MFA is currently enabled for the account.
     */
    @Transactional(readOnly = true)
    public AccountSecurityResponse getAccountSecurity(UUID userId) {
        User user = findUser(userId);

        return new AccountSecurityResponse(user.isMfaEnabled());
    }

    /**
     * Creates a new MFA secret and provisioning URI, then stores the secret until the user confirms a valid code.
     */
    @Transactional
    public MfaSetupResponse setupMfa(UUID userId) {
        User user = findUser(userId);

        if (user.isMfaEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "MFA is already enabled"
            );
        }

        String secret = mfaTotpService.generateSecret();

        String provisioningUri = mfaTotpService.generateProvisioningUri(user.getEmail(), secret);

        user.setMfaSecret(secret);
        user.setMfaEnabled(false);

        publishAuthenticationEvent(user.getId(), AuthenticationAction.MFA_SETUP, AuthenticationOutcome.STARTED);

        return new MfaSetupResponse(provisioningUri);
    }

    /**
     * Checks a code against the pending MFA secret and enables MFA when the code is valid.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void confirmMfa(UUID userId, MfaConfirmRequest request) {
        User user = findUser(userId);

        if (user.isMfaEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MFA is already enabled");
        }

        if (user.getMfaSecret() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MFA setup has not been started");
        }

        boolean valid = mfaTotpService.isValidCode(user.getMfaSecret(), request.code());

        if (!valid) {
            publishAuthenticationEvent(user.getId(), AuthenticationAction.MFA_SETUP, AuthenticationOutcome.FAILED);

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid MFA code");
        }

        user.setMfaEnabled(true);

        publishAuthenticationEvent(user.getId(), AuthenticationAction.MFA_SETUP, AuthenticationOutcome.ENABLED);
    }

    /**
     * Checks the current MFA code, removes the stored secret and invalidates any unused MFA login challenges.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void disableMfa(UUID userId, MfaConfirmRequest request) {
        User user = findUser(userId);

        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MFA is not enabled");
        }

        boolean valid = mfaTotpService.isValidCode(user.getMfaSecret(), request.code());

        if (!valid) {
            publishAuthenticationEvent(user.getId(), AuthenticationAction.MFA_DISABLE, AuthenticationOutcome.FAILED);

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid MFA code");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);

        mfaChallengeRepository.invalidateUnusedChallenges(user.getId());

        publishAuthenticationEvent(user.getId(), AuthenticationAction.MFA_DISABLE, AuthenticationOutcome.DISABLED);
    }

    /**
     * Locks and validates the pending MFA challenge, checks the code and returns an access token when the
     * challenge succeeds.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public LoginResponse completeMfaLogin(MfaLoginRequest request) {
        // Lock the challenge while it is checked so two requests cannot complete the same MFA challenge
        // at the same time.
        MfaChallenge challenge = mfaChallengeRepository.findByIdForUpdate(request.mfaChallengeId()).orElse(null);

        if (challenge == null) {
            publishAuthenticationEvent(null, AuthenticationAction.MFA_LOGIN, AuthenticationOutcome.FAILED);

            throw invalidMfaChallenge();
        }

        User user = challenge.getUser();
        Instant now = Instant.now();

        if (challenge.isUsed() || !challenge.getExpiresAt()
                .isAfter(now) || challenge.getFailedAttempts() >= MAX_MFA_ATTEMPTS) {
            publishAuthenticationEvent(user.getId(), AuthenticationAction.MFA_LOGIN, AuthenticationOutcome.FAILED);

            throw invalidMfaChallenge();
        }

        if (!user.isEnabled() || !user.isEmailVerified() || !user.isMfaEnabled() || user.getMfaSecret() == null) {
            challenge.setUsed(true);

            publishAuthenticationEvent(user.getId(), AuthenticationAction.MFA_LOGIN, AuthenticationOutcome.FAILED);

            throw invalidMfaChallenge();
        }

        boolean valid = mfaTotpService.isValidCode(user.getMfaSecret(), request.code());

        if (!valid) {
            int attempts = challenge.getFailedAttempts() + 1;
            challenge.setFailedAttempts(attempts);

            if (attempts >= MAX_MFA_ATTEMPTS) {
                challenge.setUsed(true);
            }

            publishAuthenticationEvent(user.getId(), AuthenticationAction.MFA_LOGIN, AuthenticationOutcome.FAILED);

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid MFA code"
            );
        }

        challenge.setUsed(true);

        String accessToken = jwtService.generateAccessToken(user);

        publishAuthenticationEvent(user.getId(), AuthenticationAction.MFA_LOGIN, AuthenticationOutcome.SUCCEEDED);

        return LoginResponse.authenticated(accessToken);
    }

    /**
     * Loads a user by ID or returns not found when the account does not exist.
     */
    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found")
                );
    }

    /**
     * Publishes the authentication action and outcome used by audit and analytics listeners.
     */
    private void publishAuthenticationEvent(UUID userId, AuthenticationAction action, AuthenticationOutcome outcome) {
        appEventPublisher.publish(AuthenticationEvent.create(userId, action, outcome));
    }

    /**
     * Creates the common unauthorized response used for an invalid email or password.
     */
    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
        );
    }

    /**
     * Creates the common unauthorized response used for an invalid or expired MFA challenge.
     */
    private ResponseStatusException invalidMfaChallenge() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid or expired MFA challenge"
        );
    }
}