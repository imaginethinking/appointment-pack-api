package net.imaginethinking.appointmentpack.auth.token;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines the database queries used for account tokens.
 */
public interface AccountTokenRepository extends JpaRepository<AccountToken, UUID> {

    /**
     * Locks and loads the account token matching the hash and purpose so it can be consumed safely.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token
            from AccountToken token
            join fetch token.user
            where token.tokenHash = :tokenHash
              and token.purpose = :purpose
            """)
    Optional<AccountToken> findForConsumption(
            @Param("tokenHash") String tokenHash,
            @Param("purpose") AccountTokenPurpose purpose
    );

    /**
     * Loads the matching account tokens.
     */
    List<AccountToken> findAllByUser_IdAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNull(UUID userId, AccountTokenPurpose purpose);
}