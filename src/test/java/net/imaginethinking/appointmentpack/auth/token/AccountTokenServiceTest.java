package net.imaginethinking.appointmentpack.auth.token;

import net.imaginethinking.appointmentpack.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Checks account token service behaviour across normal and failure cases.
 */
@ExtendWith(MockitoExtension.class)
class AccountTokenServiceTest {

    @Mock
    private AccountTokenRepository accountTokenRepository;

    private AccountTokenService service;

    /**
     * Creates the common fixtures and mocks used by each test.
     */
    @BeforeEach
    void setUp() {
        service = new AccountTokenService(accountTokenRepository);
    }

    @Test
    void shouldIssueTokenAndInvalidatePreviousActiveToken() {
        User user = user();

        AccountToken previous = new AccountToken(
                user,
                AccountTokenPurpose.PASSWORD_RESET,
                "previous-hash",
                Instant.now().plusSeconds(3600));

        when(accountTokenRepository.findAllByUser_IdAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNull(
                user.getId(),
                AccountTokenPurpose.PASSWORD_RESET)).thenReturn(List.of(previous));

        IssuedAccountToken issued = service.issue(user, AccountTokenPurpose.PASSWORD_RESET, Duration.ofMinutes(30));

        ArgumentCaptor<AccountToken> captor = ArgumentCaptor.forClass(AccountToken.class);

        verify(accountTokenRepository).save(captor.capture());

        AccountToken persisted = captor.getValue();

        assertNotNull(previous.getInvalidatedAt());
        assertTrue(!issued.token().isBlank());
        assertEquals(64, persisted.getTokenHash().length());
        assertNotEquals(issued.token(), persisted.getTokenHash());
        assertEquals(user, persisted.getUser());
    }

    @Test
    void shouldConsumeUsableTokenOnce() {
        User user = user();

        AccountToken token = new AccountToken(
                user,
                AccountTokenPurpose.EMAIL_VERIFICATION,
                "stored-hash",
                Instant.now().plusSeconds(300));

        when(accountTokenRepository.findForConsumption(
                anyString(),
                org.mockito.ArgumentMatchers.eq(AccountTokenPurpose.EMAIL_VERIFICATION))).thenReturn(Optional.of(token));

        Optional<User> consumed = service.consume("raw-token", AccountTokenPurpose.EMAIL_VERIFICATION);

        assertTrue(consumed.isPresent());
        assertEquals(user, consumed.orElseThrow());
        assertNotNull(token.getUsedAt());
    }

    @Test
    void shouldRejectExpiredToken() {
        User user = user();

        AccountToken token = new AccountToken(
                user,
                AccountTokenPurpose.PASSWORD_RESET,
                "stored-hash",
                Instant.now().minusSeconds(1));

        when(accountTokenRepository.findForConsumption(
                anyString(),
                org.mockito.ArgumentMatchers.eq(AccountTokenPurpose.PASSWORD_RESET))).thenReturn(Optional.of(token));

        Optional<User> consumed = service.consume("raw-token", AccountTokenPurpose.PASSWORD_RESET);

        assertTrue(consumed.isEmpty());
    }

    @Test
    void shouldRejectBlankTokenWithoutRepositoryLookup() {
        Optional<User> consumed = service.consume("   ", AccountTokenPurpose.PASSWORD_RESET);

        assertTrue(consumed.isEmpty());

        verify(accountTokenRepository, never()).findForConsumption(anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectNonPositiveTokenLifetime() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.issue(user(), AccountTokenPurpose.PASSWORD_RESET, Duration.ZERO));
    }

    /**
     * Creates a test user with the standard values used by these tests.
     */
    private User user() {
        User user = new User();

        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        return user;
    }
}