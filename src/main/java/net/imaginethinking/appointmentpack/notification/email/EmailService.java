package net.imaginethinking.appointmentpack.notification.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendBaseUrl;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${appointment-pack.email.from}") String fromAddress,
            @Value("${appointment-pack.frontend.base-url}") String frontendBaseUrl) {
        this.mailSender = mailSender;

        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalArgumentException("Email from address must not be blank");
        }

        if (frontendBaseUrl == null || frontendBaseUrl.isBlank()) {
            throw new IllegalArgumentException("Frontend base URL must not be blank");
        }

        this.fromAddress = fromAddress.strip();
        this.frontendBaseUrl = frontendBaseUrl.strip().replaceAll("/+$", "");
    }

    public void sendEmailVerification(String recipientEmail, String token) {
        String verificationUrl = buildFrontendUrl("/verify-email", token);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject("Verify your email address - The Appointment Pack");
        message.setText("""
                Welcome to The Appointment Pack.
                
                Verify your email address using the link below:
                
                %s
                
                This link expires in 24 hours. If you did not create this account, you can ignore this email.
                """.formatted(verificationUrl));

        mailSender.send(message);
    }

    public void sendPasswordReset(String recipientEmail, String token) {
        String resetUrl = buildFrontendUrl("/reset-password", token);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject("Reset your password - The Appointment Pack");
        message.setText("""
                A password reset was requested for your The Appointment Pack account.
                
                Set a new password using the link below:
                
                %s
                
                This link expires in 30 minutes. If you did not request a password reset, you can ignore this email.
                """.formatted(resetUrl));

        mailSender.send(message);
    }

    private String buildFrontendUrl(String path, String token) {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path(path)
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();
    }
}