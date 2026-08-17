package net.imaginethinking.appointmentpack.auth;

import net.imaginethinking.appointmentpack.auth.mfa.MfaChallengeRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountTokenService accountTokenService;

    @Mock
    private MfaChallengeRepository mfaChallengeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AppEventPublisher appEventPublisher;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                userRepository,
                accountTokenService,
                mfaChallengeRepository,
                passwordEncoder,
                appEventPublisher);
    }

    @Test
    void shouldIssueResetTokenForVerifiedEnabledUser() {
        User user = verifiedUser();

        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(user));

        when(accountTokenService.issue(
                eq(user),
                eq(AccountTokenPurpose.PASSWORD_RESET),
                any())).thenReturn(new IssuedAccountToken("reset-token", Instant.now().plusSeconds(1800)));

        service.requestReset(new PasswordResetRequest("Patient@Example.com"));

        verify(accountTokenService).issue(eq(user), eq(AccountTokenPurpose.PASSWORD_RESET), any());

        verify(appEventPublisher, times(2)).publish(any());
    }

    @Test
    void shouldRemainEnumerationSafeForUnknownUser() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.requestReset(new PasswordResetRequest("unknown@example.com"));

        verify(accountTokenService, never()).issue(any(), any(), any());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldResetPasswordAndInvalidateOutstandingLoginState() {
        User user = verifiedUser();

        when(accountTokenService.consume(
                "reset-token",
                AccountTokenPurpose.PASSWORD_RESET)).thenReturn(Optional.of(user));

        when(passwordEncoder.encode("Replacement1!")).thenReturn("new-hash");

        service.confirmReset(new PasswordResetConfirmRequest("reset-token", "Replacement1!", "Replacement1!"));

        assertEquals("new-hash", user.getPasswordHash());

        verify(accountTokenService).invalidateActiveTokens(user, AccountTokenPurpose.PASSWORD_RESET);

        verify(mfaChallengeRepository).invalidateUnusedChallenges(user.getId());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldRejectMismatchedNewPasswordsBeforeConsumingToken() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.confirmReset(new PasswordResetConfirmRequest(
                        "reset-token",
                        "Replacement1!",
                        "Different1!")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(accountTokenService, never()).consume(any(), any());
    }

    @Test
    void shouldRejectInvalidResetToken() {
        when(accountTokenService.consume(
                "invalid-token",
                AccountTokenPurpose.PASSWORD_RESET)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.confirmReset(new PasswordResetConfirmRequest(
                        "invalid-token",
                        "Replacement1!",
                        "Replacement1!")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private User verifiedUser() {
        User user = new User();

        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        user.setEmail("patient@example.com");
        user.setPasswordHash("old-hash");
        user.setEnabled(true);
        user.setEmailVerifiedAt(Instant.now());

        return user;
    }
}