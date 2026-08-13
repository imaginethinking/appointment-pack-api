package net.imaginethinking.appointmentpack.auth.token;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountTokenService {

    private static final int TOKEN_BYTES = 32;

    private final AccountTokenRepository accountTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public IssuedAccountToken issue(User user, AccountTokenPurpose purpose, Duration lifetime) {
        if (lifetime == null || lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("Token lifetime must be positive");
        }

        Instant now = Instant.now();

        invalidateActiveTokens(user, purpose, now);

        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken);
        Instant expiresAt = now.plus(lifetime);

        AccountToken accountToken = new AccountToken(user, purpose, tokenHash, expiresAt);

        accountTokenRepository.save(accountToken);

        return new IssuedAccountToken(rawToken, expiresAt);
    }

    @Transactional
    public Optional<User> consume(String rawToken, AccountTokenPurpose purpose) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        String tokenHash = hashToken(rawToken.strip());

        Optional<AccountToken> accountToken = accountTokenRepository.findForConsumption(tokenHash, purpose);

        if (accountToken.isEmpty()) {
            return Optional.empty();
        }

        AccountToken token = accountToken.get();
        Instant now = Instant.now();

        if (!token.isUsable(now)) {
            return Optional.empty();
        }

        token.markUsed(now);

        return Optional.of(token.getUser());
    }

    @Transactional
    public void invalidateActiveTokens(User user, AccountTokenPurpose purpose) {
        invalidateActiveTokens(user, purpose, Instant.now());
    }

    private void invalidateActiveTokens(User user, AccountTokenPurpose purpose, Instant invalidatedAt) {
        accountTokenRepository.findAllByUser_IdAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNull(user.getId(), purpose)
                .forEach(token -> token.invalidate(invalidatedAt));
    }

    private String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}