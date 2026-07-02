package net.imaginethinking.appointmentpack.appointment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.facility.Facility;
import net.imaginethinking.appointmentpack.user.User;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "appointments")
public class Appointment extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentType type;

    // Medical staff who is conducting the appointment
    @Column
    private String clinician;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

//  Lazy fetching so it does not fetch facility details until they are needed
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "facility_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_facility")
    )
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    @Column
    private String notes;
}
