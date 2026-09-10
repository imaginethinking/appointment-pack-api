package net.imaginethinking.appointmentpack.auth;

import net.imaginethinking.appointmentpack.auth.token.AccountTokenPurpose;
import net.imaginethinking.appointmentpack.auth.token.AccountTokenService;
import net.imaginethinking.appointmentpack.auth.token.IssuedAccountToken;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.user.User;
import net.imaginethinking.appointmentpack.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Checks email verification service behaviour across normal and failure cases.
 */
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountTokenService accountTokenService;

    @Mock
    private AppEventPublisher appEventPublisher;

    private EmailVerificationService service;

    /**
     * Creates the common fixtures and mocks used by each test.
     */
    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(userRepository, accountTokenService, appEventPublisher);
    }

    @Test
    void shouldIssueInitialVerificationTokenAndEvents() {
        User user = user(false);

        when(accountTokenService.issue(
                eq(user),
                eq(AccountTokenPurpose.EMAIL_VERIFICATION),
                any())).thenReturn(new IssuedAccountToken("verification-token", Instant.now().plusSeconds(3600)));

        service.issueInitialVerification(user);

        verify(accountTokenService).issue(eq(user), eq(AccountTokenPurpose.EMAIL_VERIFICATION), any());

        verify(appEventPublisher, times(2)).publish(any());
    }

    @Test
    void shouldNotIssueResendForUnknownUser() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.resend(new EmailVerificationResendRequest("unknown@example.com"));

        verify(accountTokenService, never()).issue(any(), any(), any());
    }

    @Test
    void shouldVerifyUserWhenTokenIsValid() {
        User user = user(false);

        when(accountTokenService.consume("verification-token", AccountTokenPurpose.EMAIL_VERIFICATION)).thenReturn(
                Optional.of(user));

        service.confirm(new EmailVerificationConfirmRequest("verification-token"));

        assertNotNull(user.getEmailVerifiedAt());
        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldRejectInvalidVerificationToken() {
        when(accountTokenService.consume(
                "invalid-token",
                AccountTokenPurpose.EMAIL_VERIFICATION)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.confirm(new EmailVerificationConfirmRequest("invalid-token")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(appEventPublisher).publish(any());
    }

    /**
     * Creates a test user with the supplied values.
     */
    private User user(boolean verified) {
        User user = new User();

        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        user.setEmail("patient@example.com");
        user.setEnabled(true);

        if (verified) {
            user.setEmailVerifiedAt(Instant.now());
        }

        return user;
    }
}