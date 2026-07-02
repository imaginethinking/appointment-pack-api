package net.imaginethinking.appointmentpack.medication;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.user.User;

@Getter
@Setter
@Entity
@Table(name = "medications")
public class Medication extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column
    private String dose;

    @Column
    private String startDate;

    @Column
    private String endDate;

    @Column
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;
}
