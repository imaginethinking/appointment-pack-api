package net.imaginethinking.appointmentpack.auth.mfa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MfaChallengeRepository extends JpaRepository<MfaChallenge, UUID> {
    @Modifying
    @Query("""
            update MfaChallenge challenge
            set challenge.used = true
            where challenge.user.id = :userId
              and challenge.used = false
            """)
    int invalidateUnusedChallenges(@Param("userId") UUID userId);
}
