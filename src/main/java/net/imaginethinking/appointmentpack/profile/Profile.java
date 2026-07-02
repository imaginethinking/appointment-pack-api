package net.imaginethinking.appointmentpack.profile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.address.Address;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.user.User;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "profiles")
public class Profile extends BaseEntity {

    @OneToOne()
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
            name = "address_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "fk_address")
    )
    private Address address;

    // England and Wales regional number
    @Column(name = "nhs_number", nullable = true, length = 10)
    private String nhsNumber;

    // Scotland regional number
    @Column(name = "chi_cnumber", nullable = true, length = 10)
    private String chiNumber;

    // Northern Ireland regional number
    @Column(name = "hc_number", nullable = true, length = 10)
    private String hcNumber;
}
