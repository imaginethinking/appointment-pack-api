package net.imaginethinking.appointmentpack.medication;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "medications")
public class Medication extends BaseEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "patient_record_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_medication_patient_record"
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
            name = "dose",
            length = 100
    )
    private String dose;

    @Column(
            name = "form",
            length = 100
    )
    private String form;

    @Column(
            name = "instructions",
            length = 500
    )
    private String instructions;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

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