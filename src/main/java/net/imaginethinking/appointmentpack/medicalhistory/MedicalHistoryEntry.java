package net.imaginethinking.appointmentpack.medicalhistory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.ArchivableEntity;
import net.imaginethinking.appointmentpack.document.Document;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.user.User;

import java.time.LocalDate;

/**
 * Stores a manual or document based entry in a patient's Medical History.
 */
@Getter
@Setter
@Entity
@Table(
        name = "medical_history_entries",
        indexes = {@Index(
                name = "idx_medical_history_patient_date",
                columnList = "patient_record_id, entry_date"
        )},
        uniqueConstraints = {@UniqueConstraint(
                name = "uk_medical_history_source_document",
                columnNames = "source_document_id"
        )}
)
public class MedicalHistoryEntry extends ArchivableEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "patient_record_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_medical_history_patient_record"
            )
    )
    private PatientRecord patientRecord;

    @Column(
            name = "title",
            nullable = false,
            length = 200
    )
    private String title;

    @Column(
            name = "summary",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String summary;

    @Column(
            name = "entry_date",
            nullable = false
    )
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "source_type",
            nullable = false,
            length = 50
    )
    private MedicalHistorySourceType sourceType;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "source_document_id",
            foreignKey = @ForeignKey(
                    name = "fk_medical_history_source_document"
            )
    )
    private Document sourceDocument;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "created_by_user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_medical_history_created_by"
            )
    )
    private User createdBy;
}