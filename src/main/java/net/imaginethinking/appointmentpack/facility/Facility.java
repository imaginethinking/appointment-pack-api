package net.imaginethinking.appointmentpack.facility;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.address.Address;
import net.imaginethinking.appointmentpack.common.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "facilities")
public class Facility extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "address_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_address")
    )
    private Address address;
}
