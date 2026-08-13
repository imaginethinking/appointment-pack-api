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

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration PASSWORD_RESET_TOKEN_LIFETIME = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final AccountTokenService accountTokenService;
    private final MfaChallengeRepository mfaChallengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppEventPublisher appEventPublisher;

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
}