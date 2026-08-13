package net.imaginethinking.appointmentpack.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.profile.Profile;

import java.time.Instant;

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

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }
}