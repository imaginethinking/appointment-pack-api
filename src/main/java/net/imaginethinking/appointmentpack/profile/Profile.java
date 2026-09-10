package net.imaginethinking.appointmentpack.profile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.address.Address;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.user.User;

import java.time.LocalDate;

/**
 * Stores the personal profile linked to a user account.
 */
@Getter
@Setter
@Entity
@Table(name = "profiles")
public class Profile extends BaseEntity {

    @OneToOne()
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_profile_user")
    )
    private User user;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @Embedded
    private Address address;
}
