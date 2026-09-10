package net.imaginethinking.appointmentpack.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.profile.Profile;

import java.time.Instant;

/**
 * Stores account credentials, roles and account security state for an application user.
 */
@Getter
@Setter
@Entity
@Table(
        name = "users",
        uniqueConstraints = {@UniqueConstraint(
                name = "uk_user_email",
                columnNames = "email"
        )}
)
public class User extends BaseEntity {

    @Column(
            name = "email",
            nullable = false,
            length = 254
    )
    private String email;

    @Column(
            name = "password_hash",
            nullable = false
    )
    private String passwordHash;

    @Column(
            name = "enabled",
            nullable = false
    )
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "role",
            nullable = false,
            length = 20
    )
    private UserRole role = UserRole.USER;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(
            name = "mfa_enabled",
            nullable = false
    )
    private boolean mfaEnabled;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            optional = false
    )
    private Profile profile;

    /**
     * Checks whether the account has an email verification time recorded.
     */
    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }
}