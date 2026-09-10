package net.imaginethinking.appointmentpack.notification.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * Checks email service behaviour across normal and failure cases.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService service;

    /**
     * Creates the common fixtures and mocks used by each test.
     */
    @BeforeEach
    void setUp() {
        service = new EmailService(mailSender, " no-reply@appointment-pack.test ", " https://frontend.example/ ");
    }

    @Test
    void shouldBuildAndSendEmailVerificationMessage() {
        service.sendEmailVerification("patient@example.com", "verification-token");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertEquals("no-reply@appointment-pack.test", message.getFrom());
        assertEquals("patient@example.com", message.getTo()[0]);
        assertEquals("Verify your email address - The Appointment Pack", message.getSubject());
        assertTrue(message.getText().contains("https://frontend.example/verify-email?token=verification-token"));
        assertTrue(message.getText().contains("expires in 24 hours"));
    }

    @Test
    void shouldBuildAndSendPasswordResetMessage() {
        service.sendPasswordReset("patient@example.com", "reset-token");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertEquals("Reset your password - The Appointment Pack", message.getSubject());
        assertTrue(message.getText().contains("https://frontend.example/reset-password?token=reset-token"));
        assertTrue(message.getText().contains("expires in 30 minutes"));
    }
}