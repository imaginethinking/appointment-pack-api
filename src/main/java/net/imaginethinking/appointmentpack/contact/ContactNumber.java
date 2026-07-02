package net.imaginethinking.appointmentpack.contact;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "contact_numbers")
public class ContactNumber extends BaseEntity {

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "contact_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_contact_number_contact")
    )
    private Contact contact;

}
