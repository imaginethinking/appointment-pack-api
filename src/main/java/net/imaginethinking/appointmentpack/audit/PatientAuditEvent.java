package net.imaginethinking.appointmentpack.audit;

import jakarta.persistence.*;
import lombok.Getter;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Stores a patient activity entry created from a significant application event.
 */
@Getter
@Entity
@Immutable
@Table(
        name = "patient_audit_events",
        indexes = {@Index(
                name = "idx_patient_audit_patient_occurred",
                columnList = "patient_record_id, occurred_at"
        ), @Index(
                name = "idx_patient_audit_actor",
                columnList = "actor_user_id"
        )},
        uniqueConstraints = {@UniqueConstraint(
                name = "uk_patient_audit_source_event",
                columnNames = "source_event_id"
        )}
)
public class PatientAuditEvent {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "source_event_id",
            nullable = false,
            updatable = false
    )
    private UUID sourceEventId;

    @Column(
            name = "patient_record_id",
            nullable = false,
            updatable = false
    )
    private UUID patientRecordId;

    @Column(
            name = "actor_user_id",
            nullable = false,
            updatable = false
    )
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "resource_type",
            nullable = false,
            updatable = false,
            length = 50
    )
    private PatientResourceType resourceType;

    @Column(
            name = "resource_id",
            nullable = false,
            updatable = false
    )
    private UUID resourceId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "action",
            nullable = false,
            updatable = false,
            length = 50
    )
    private PatientActivityAction action;

    @Column(
            name = "occurred_at",
            nullable = false,
            updatable = false
    )
    private Instant occurredAt;

    /**
     * Creates the persistence object used by JPA.
     */
    protected PatientAuditEvent() {
    }

    /**
     * Creates the persistence object used by JPA.
     */
    public PatientAuditEvent(
            UUID sourceEventId,
            UUID patientRecordId,
            UUID actorUserId,
            PatientResourceType resourceType,
            UUID resourceId,
            PatientActivityAction action,
            Instant occurredAt) {
        this.id = UUID.randomUUID();
        this.sourceEventId = Objects.requireNonNull(sourceEventId, "Source event ID must not be null");
        this.patientRecordId = Objects.requireNonNull(patientRecordId, "Patient record ID must not be null");
        this.actorUserId = Objects.requireNonNull(actorUserId, "Actor user ID must not be null");
        this.resourceType = Objects.requireNonNull(resourceType, "Resource type must not be null");
        this.resourceId = Objects.requireNonNull(resourceId, "Resource ID must not be null");
        this.action = Objects.requireNonNull(action, "Audit action must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurrence timestamp must not be null");
    }
}