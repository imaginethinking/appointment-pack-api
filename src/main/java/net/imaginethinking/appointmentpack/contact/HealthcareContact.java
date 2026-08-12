package net.imaginethinking.appointmentpack.contact;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.address.Address;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;

@Getter
@Setter
@Entity
@Table(name = "healthcare_contacts")
public class HealthcareContact extends BaseEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "patient_record_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_healthcare_contact_patient_record"
            )
    )
    private PatientRecord patientRecord;

    @Column(
            name = "name",
            nullable = false,
            length = 200
    )
    private String name;

    @Column(
            name = "role",
            length = 150
    )
    private String role;

    @Column(
            name = "organisation",
            length = 200
    )
    private String organisation;

    @Column(
            name = "phone_number",
            length = 50
    )
    private String phoneNumber;

    @Column(
            name = "email",
            length = 254
    )
    private String email;

    @Embedded
    private Address address;

    @Column(
            name = "notes",
            length = 2000
    )
    private String notes;

    @Column(
            name = "archived",
            nullable = false
    )
    private boolean archived;
}