package net.imaginethinking.appointmentpack.auth.mfa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.user.User;

import java.time.Instant;

/**
 * Stores an MFA login challenge and its failed attempt count.
 */
@Getter
@Setter
@Entity
@Table(name = "mfa_challenges")
public class MfaChallenge extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_mfa_challenge_user")
    )
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;
}
