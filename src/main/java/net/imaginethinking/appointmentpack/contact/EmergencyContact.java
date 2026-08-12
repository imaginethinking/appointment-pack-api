package net.imaginethinking.appointmentpack.contact;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;

@Getter
@Setter
@Entity
@Table(name = "emergency_contacts")
public class EmergencyContact extends BaseEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "patient_record_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_emergency_contact_patient_record"
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
            name = "relationship",
            nullable = false,
            length = 150
    )
    private String relationship;

    @Column(
            name = "phone_number",
            nullable = false,
            length = 50
    )
    private String phoneNumber;

    @Column(
            name = "alternative_phone_number",
            length = 50
    )
    private String alternativePhoneNumber;

    @Column(
            name = "email",
            length = 254
    )
    private String email;

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