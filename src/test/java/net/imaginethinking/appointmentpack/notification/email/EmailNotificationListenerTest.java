package net.imaginethinking.appointmentpack.notification.email;

import net.imaginethinking.appointmentpack.event.notification.EmailVerificationEmailRequestedEvent;
import net.imaginethinking.appointmentpack.event.notification.PasswordResetEmailRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationListenerTest {

    @Mock
    private EmailService emailService;

    private EmailNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new EmailNotificationListener(emailService);
    }

    @Test
    void shouldDispatchEmailVerificationRequest() {
        EmailVerificationEmailRequestedEvent event = EmailVerificationEmailRequestedEvent.create(
                UUID.randomUUID(),
                "patient@example.com",
                "verification-token",
                Instant.now().plusSeconds(3600));

        listener.handleEmailVerification(event);

        verify(emailService).sendEmailVerification("patient@example.com", "verification-token");
    }

    @Test
    void shouldDispatchPasswordResetRequest() {
        PasswordResetEmailRequestedEvent event = PasswordResetEmailRequestedEvent.create(
                UUID.randomUUID(),
                "patient@example.com",
                "reset-token",
                Instant.now().plusSeconds(1800));

        listener.handlePasswordReset(event);

        verify(emailService).sendPasswordReset("patient@example.com", "reset-token");
    }

    @Test
    void shouldNotPropagateEmailDeliveryFailure() {
        EmailVerificationEmailRequestedEvent event = EmailVerificationEmailRequestedEvent.create(
                UUID.randomUUID(),
                "patient@example.com",
                "verification-token",
                Instant.now().plusSeconds(3600));

        doThrow(new RuntimeException("smtp unavailable")).when(emailService)
                .sendEmailVerification("patient@example.com", "verification-token");

        assertDoesNotThrow(() -> listener.handleEmailVerification(event));
    }
}