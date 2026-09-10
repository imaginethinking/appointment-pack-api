package net.imaginethinking.appointmentpack.auth.mfa;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Defines the database queries used for MFA challenges.
 */
public interface MfaChallengeRepository extends JpaRepository<MfaChallenge, UUID> {

    /**
     * Locks and loads the MFA challenge so concurrent requests cannot complete the same challenge twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select challenge
            from MfaChallenge challenge
            join fetch challenge.user
            where challenge.id = :challengeId
            """)
    Optional<MfaChallenge> findByIdForUpdate(@Param("challengeId") UUID challengeId);

    /**
     * Marks unused MFA challenges for the account as used so they can no longer complete a login.
     */
    @Modifying
    @Query("""
            update MfaChallenge challenge
            set challenge.used = true
            where challenge.user.id = :userId
              and challenge.used = false
            """)
    int invalidateUnusedChallenges(@Param("userId") UUID userId);
}