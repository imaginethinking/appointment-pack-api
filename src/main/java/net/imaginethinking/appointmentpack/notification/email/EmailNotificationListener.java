package net.imaginethinking.appointmentpack.notification.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.imaginethinking.appointmentpack.event.notification.EmailVerificationEmailRequestedEvent;
import net.imaginethinking.appointmentpack.event.notification.PasswordResetEmailRequestedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmailVerification(EmailVerificationEmailRequestedEvent event) {
        try {
            emailService.sendEmailVerification(event.recipientEmail(), event.token());
        } catch (RuntimeException exception) {
            log.error("Failed to send email verification message for user {}", event.userId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordReset(PasswordResetEmailRequestedEvent event) {
        try {
            emailService.sendPasswordReset(event.recipientEmail(), event.token());
        } catch (RuntimeException exception) {
            log.error("Failed to send password reset message for user {}", event.userId(), exception);
        }
    }
}