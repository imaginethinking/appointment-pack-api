package net.imaginethinking.appointmentpack.auth.token;

import jakarta.persistence.*;
import lombok.Getter;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.user.User;

import java.time.Instant;
import java.util.Objects;

/**
 * Stores the hashed one time token used for email verification or password reset.
 */
@Getter
@Entity
@Table(
        name = "account_tokens",
        indexes = {@Index(
                name = "idx_account_token_user_purpose",
                columnList = "user_id, purpose"
        )},
        uniqueConstraints = {@UniqueConstraint(
                name = "uk_account_token_hash",
                columnNames = "token_hash"
        )}
)
public class AccountToken extends BaseEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_account_token_user"
            )
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "purpose",
            nullable = false,
            length = 50
    )
    private AccountTokenPurpose purpose;

    @Column(
            name = "token_hash",
            nullable = false,
            length = 64
    )
    private String tokenHash;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    /**
     * Creates the account token persistence object.
     */
    protected AccountToken() {
    }

    /**
     * Creates the account token persistence object.
     */
    public AccountToken(User user, AccountTokenPurpose purpose, String tokenHash, Instant expiresAt) {
        this.user = Objects.requireNonNull(user, "User must not be null");
        this.purpose = Objects.requireNonNull(purpose, "Token purpose must not be null");
        this.tokenHash = Objects.requireNonNull(tokenHash, "Token hash must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Token expiry must not be null");
    }

    /**
     * Checks that the token is unused, has not been invalidated and has not expired.
     */
    public boolean isUsable(Instant now) {
        return usedAt == null && invalidatedAt == null && expiresAt.isAfter(now);
    }

    /**
     * Records when the token was successfully consumed.
     */
    public void markUsed(Instant usedAt) {
        if (this.usedAt == null) {
            this.usedAt = Objects.requireNonNull(usedAt, "Used timestamp must not be null");
        }
    }

    /**
     * Records when the token was invalidated so it cannot be used later.
     */
    public void invalidate(Instant invalidatedAt) {
        if (usedAt == null && this.invalidatedAt == null) {
            this.invalidatedAt = Objects.requireNonNull(invalidatedAt, "Invalidation timestamp must not be null");
        }
    }
}