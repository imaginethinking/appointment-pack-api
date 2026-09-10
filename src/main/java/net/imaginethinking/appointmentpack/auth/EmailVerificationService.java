package net.imaginethinking.appointmentpack.auth;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.auth.token.AccountTokenPurpose;
import net.imaginethinking.appointmentpack.auth.token.AccountTokenService;
import net.imaginethinking.appointmentpack.auth.token.IssuedAccountToken;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationAction;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationEvent;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationOutcome;
import net.imaginethinking.appointmentpack.event.notification.EmailVerificationEmailRequestedEvent;
import net.imaginethinking.appointmentpack.user.EmailAddressNormalizer;
import net.imaginethinking.appointmentpack.user.User;
import net.imaginethinking.appointmentpack.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

/**
 * Issues and confirms email verification tokens and publishes the email request when a new token is created.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Duration VERIFICATION_TOKEN_LIFETIME = Duration.ofHours(24);

    private final UserRepository userRepository;
    private final AccountTokenService accountTokenService;
    private final AppEventPublisher appEventPublisher;

    /**
     * Issues the first email verification token for a newly registered account.
     */
    @Transactional
    public void issueInitialVerification(User user) {
        issueVerification(user, AuthenticationOutcome.REQUESTED);
    }

    /**
     * Issues another verification email only when the account exists, is enabled and is still unverified.
     */
    @Transactional
    public void resend(EmailVerificationResendRequest request) {
        String email = EmailAddressNormalizer.normalise(request.email());

        userRepository.findByEmail(email)
                .filter(User::isEnabled)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(user -> issueVerification(user, AuthenticationOutcome.RESENT));
    }

    /**
     * Consumes the verification token and marks the account email as verified when the token is valid.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void confirm(EmailVerificationConfirmRequest request) {
        User user = accountTokenService.consume(request.token(), AccountTokenPurpose.EMAIL_VERIFICATION).orElse(null);

        if (user == null) {
            appEventPublisher.publish(AuthenticationEvent.create(
                    null,
                    AuthenticationAction.EMAIL_VERIFICATION,
                    AuthenticationOutcome.FAILED));

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired email verification token");
        }

        if (!user.isEnabled()) {
            appEventPublisher.publish(AuthenticationEvent.create(
                    user.getId(),
                    AuthenticationAction.EMAIL_VERIFICATION,
                    AuthenticationOutcome.FAILED));

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired email verification token");
        }

        if (!user.isEmailVerified()) {
            user.setEmailVerifiedAt(Instant.now());
        }

        appEventPublisher.publish(AuthenticationEvent.create(
                user.getId(),
                AuthenticationAction.EMAIL_VERIFICATION,
                AuthenticationOutcome.SUCCEEDED));
    }

    /**
     * Creates a verification token and publishes the event used to send it to the user.
     */
    private void issueVerification(User user, AuthenticationOutcome outcome) {
        IssuedAccountToken issuedToken = accountTokenService.issue(
                user,
                AccountTokenPurpose.EMAIL_VERIFICATION,
                VERIFICATION_TOKEN_LIFETIME);

        appEventPublisher.publish(AuthenticationEvent.create(
                user.getId(),
                AuthenticationAction.EMAIL_VERIFICATION,
                outcome));

        appEventPublisher.publish(EmailVerificationEmailRequestedEvent.create(
                user.getId(),
                user.getEmail(),
                issuedToken.token(),
                issuedToken.expiresAt()));
    }
}