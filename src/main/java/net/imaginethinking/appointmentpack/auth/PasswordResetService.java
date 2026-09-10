package net.imaginethinking.appointmentpack.auth;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.auth.mfa.MfaChallengeRepository;
import net.imaginethinking.appointmentpack.auth.token.AccountTokenPurpose;
import net.imaginethinking.appointmentpack.auth.token.AccountTokenService;
import net.imaginethinking.appointmentpack.auth.token.IssuedAccountToken;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationAction;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationEvent;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationOutcome;
import net.imaginethinking.appointmentpack.event.notification.PasswordResetEmailRequestedEvent;
import net.imaginethinking.appointmentpack.user.EmailAddressNormalizer;
import net.imaginethinking.appointmentpack.user.User;
import net.imaginethinking.appointmentpack.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Handles password reset requests and password changes while invalidating tokens that should no longer be used.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration PASSWORD_RESET_TOKEN_LIFETIME = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final AccountTokenService accountTokenService;
    private final MfaChallengeRepository mfaChallengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppEventPublisher appEventPublisher;

    /**
     * Records the reset request and only issues a token when the email belongs to an enabled verified account.
     */
    @Transactional
    public void requestReset(PasswordResetRequest request) {
        String email = EmailAddressNormalizer.normalise(request.email());

        User user = userRepository.findByEmail(email).orElse(null);

        appEventPublisher.publish(AuthenticationEvent.create(
                user == null ? null : user.getId(),
                AuthenticationAction.PASSWORD_RESET,
                AuthenticationOutcome.REQUESTED));

        if (user == null || !user.isEnabled() || !user.isEmailVerified()) {
            return;
        }

        IssuedAccountToken issuedToken = accountTokenService.issue(
                user,
                AccountTokenPurpose.PASSWORD_RESET,
                PASSWORD_RESET_TOKEN_LIFETIME);

        appEventPublisher.publish(PasswordResetEmailRequestedEvent.create(
                user.getId(),
                user.getEmail(),
                issuedToken.token(),
                issuedToken.expiresAt()));
    }

    /**
     * Checks the current password and new password confirmation, saves the replacement password and invalidates
     * older recovery state.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void changePassword(UUID userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!Objects.equals(request.newPassword(), request.confirmPassword())) {
            publishPasswordChangeEvent(user, AuthenticationOutcome.FAILED);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passwords do not match"
            );
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            publishPasswordChangeEvent(user, AuthenticationOutcome.FAILED);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Current password is incorrect"
            );
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            publishPasswordChangeEvent(user, AuthenticationOutcome.FAILED);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New password must be different from the current password"
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        accountTokenService.invalidateActiveTokens(user, AccountTokenPurpose.PASSWORD_RESET);
        mfaChallengeRepository.invalidateUnusedChallenges(user.getId());

        publishPasswordChangeEvent(user, AuthenticationOutcome.SUCCEEDED);
    }

    /**
     * Consumes a valid reset token, saves the new password and clears other reset or MFA challenges for the
     * account.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void confirmReset(PasswordResetConfirmRequest request) {
        if (!Objects.equals(request.newPassword(), request.confirmPassword())) {
            appEventPublisher.publish(AuthenticationEvent.create(
                    null,
                    AuthenticationAction.PASSWORD_RESET,
                    AuthenticationOutcome.FAILED));

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passwords do not match"
            );
        }

        User user = accountTokenService.consume(request.token(), AccountTokenPurpose.PASSWORD_RESET).orElse(null);

        if (user == null || !user.isEnabled() || !user.isEmailVerified()) {
            appEventPublisher.publish(AuthenticationEvent.create(
                    user == null ? null : user.getId(),
                    AuthenticationAction.PASSWORD_RESET,
                    AuthenticationOutcome.FAILED));

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or expired password reset token"
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        accountTokenService.invalidateActiveTokens(user, AccountTokenPurpose.PASSWORD_RESET);

        mfaChallengeRepository.invalidateUnusedChallenges(user.getId());

        appEventPublisher.publish(AuthenticationEvent.create(
                user.getId(),
                AuthenticationAction.PASSWORD_RESET,
                AuthenticationOutcome.SUCCEEDED));
    }

    /**
     * Records whether an authenticated password change succeeded or failed.
     */
    private void publishPasswordChangeEvent(User user, AuthenticationOutcome outcome) {
        appEventPublisher.publish(AuthenticationEvent.create(
                user.getId(),
                AuthenticationAction.PASSWORD_CHANGE,
                outcome));
    }
}