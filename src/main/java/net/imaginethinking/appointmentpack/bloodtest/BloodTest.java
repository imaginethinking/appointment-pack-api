package net.imaginethinking.appointmentpack.bloodtest;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.ArchivableEntity;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores a blood test and its ordered result rows for a patient.
 */
@Getter
@Setter
@Entity
@Table(
        name = "blood_tests",
        indexes = {@Index(
                name = "idx_blood_test_patient_date",
                columnList = "patient_record_id, test_date"
        )}
)
public class BloodTest extends ArchivableEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "patient_record_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_blood_test_patient_record"
            )
    )
    private PatientRecord patientRecord;

    @Column(
            name = "title",
            length = 200
    )
    private String title;

    @Column(
            name = "test_date",
            nullable = false
    )
    private LocalDate testDate;

    @Column(
            name = "provider",
            length = 200
    )
    private String provider;

    @Column(
            name = "notes",
            length = 2000
    )
    private String notes;

    @OneToMany(
            mappedBy = "bloodTest",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("displayOrder ASC")
    private List<BloodTestResult> results = new ArrayList<>();
}