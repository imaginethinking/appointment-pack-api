package net.imaginethinking.appointmentpack.pack;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.appointment.Appointment;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.user.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "appointment_packs")
public class AppointmentPack extends BaseEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "patient_record_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_appointment_pack_patient_record"
            )
    )
    private PatientRecord patientRecord;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "appointment_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_appointment_pack_appointment"
            )
    )
    private Appointment appointment;

    @Column(
            name = "title",
            nullable = false,
            length = 250
    )
    private String title;

    @Column(
            name = "notes",
            length = 2000
    )
    private String notes;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "generated_by_user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_appointment_pack_generated_by"
            )
    )
    private User generatedBy;

    @Column(
            name = "generated_at",
            nullable = false
    )
    private Instant generatedAt;

    @Column(
            name = "file_name",
            nullable = false,
            length = 255
    )
    private String fileName;

    @Column(
            name = "stored_file_name",
            nullable = false,
            length = 255
    )
    private String storedFileName;

    @Column(
            name = "storage_path",
            nullable = false,
            length = 500
    )
    private String storagePath;

    @Column(
            name = "content_type",
            nullable = false,
            length = 100
    )
    private String contentType;

    @Column(
            name = "file_size",
            nullable = false
    )
    private long fileSize;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @OneToMany(
            mappedBy = "appointmentPack",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("displayOrder ASC")
    private List<AppointmentPackItem> items = new ArrayList<>();
}