package net.imaginethinking.appointmentpack.patientaccess;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.user.User;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "patient_carer_access",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_patient_carer_access",
                        columnNames = {"patient_record_id", "carer_user_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_patient_carer_access_patient",
                        columnList = "patient_record_id"
                ),
                @Index(
                        name = "idx_patient_carer_access_carer",
                        columnList = "carer_user_id"
                )
        }

)
public class PatientCarerAccess extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "patient_record_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_patient_carer_access_patient")
    )
    private PatientRecord patientRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "carer_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_patient_carer_access_carer")
    )
    private User carer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PatientCarerAccessStatus status;

    @Column(name = "invited_at", nullable = false)
    private Instant invitedAt;

    @Column(name = "status_changed_at", nullable = false)
    private Instant statusChangedAt;
}
