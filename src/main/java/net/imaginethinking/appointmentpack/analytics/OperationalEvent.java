package net.imaginethinking.appointmentpack.analytics;

import jakarta.persistence.*;
import lombok.Getter;
import net.imaginethinking.appointmentpack.document.DocumentType;
import net.imaginethinking.appointmentpack.event.analytics.ApplicationPage;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationAction;
import net.imaginethinking.appointmentpack.event.auth.AuthenticationOutcome;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingFailureReason;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingOperation;
import net.imaginethinking.appointmentpack.event.processing.ProcessingOutcome;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Immutable
@Table(
        name = "operational_events",
        indexes = {@Index(
                name = "idx_operational_event_occurred",
                columnList = "occurred_at"
        ), @Index(
                name = "idx_operational_event_category_occurred",
                columnList = "category, occurred_at"
        ), @Index(
                name = "idx_operational_event_name_occurred",
                columnList = "event_name, occurred_at"
        ), @Index(
                name = "idx_operational_event_user_occurred",
                columnList = "user_id, occurred_at"
        )},
        uniqueConstraints = {@UniqueConstraint(
                name = "uk_operational_event_source_event",
                columnNames = "source_event_id"
        )}
)
public class OperationalEvent {

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

    @Enumerated(EnumType.STRING)
    @Column(
            name = "category",
            nullable = false,
            updatable = false,
            length = 50
    )
    private OperationalEventCategory category;

    @Column(
            name = "event_name",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String eventName;

    @Column(
            name = "user_id",
            updatable = false
    )
    private UUID userId;

    @Column(
            name = "occurred_at",
            nullable = false,
            updatable = false
    )
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "patient_resource_type",
            updatable = false,
            length = 50
    )
    private PatientResourceType patientResourceType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "patient_action",
            updatable = false,
            length = 50
    )
    private PatientActivityAction patientAction;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "authentication_action",
            updatable = false,
            length = 50
    )
    private AuthenticationAction authenticationAction;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "authentication_outcome",
            updatable = false,
            length = 50
    )
    private AuthenticationOutcome authenticationOutcome;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "document_type",
            updatable = false,
            length = 50
    )
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "processing_operation",
            updatable = false,
            length = 50
    )
    private DocumentProcessingOperation processingOperation;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "processing_outcome",
            updatable = false,
            length = 50
    )
    private ProcessingOutcome processingOutcome;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "processing_failure_reason",
            updatable = false,
            length = 50
    )
    private DocumentProcessingFailureReason processingFailureReason;

    @Column(
            name = "duration_ms",
            updatable = false
    )
    private Long durationMs;

    @Column(
            name = "processor_version",
            updatable = false,
            length = 100
    )
    private String processorVersion;

    @Column(
            name = "model_name",
            updatable = false,
            length = 100
    )
    private String modelName;

    @Column(
            name = "prompt_version",
            updatable = false,
            length = 100
    )
    private String promptVersion;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "page",
            updatable = false,
            length = 50
    )
    private ApplicationPage page;

    protected OperationalEvent() {
    }

    OperationalEvent(
            UUID sourceEventId,
            OperationalEventCategory category,
            String eventName,
            UUID userId,
            Instant occurredAt,
            PatientResourceType patientResourceType,
            PatientActivityAction patientAction,
            AuthenticationAction authenticationAction,
            AuthenticationOutcome authenticationOutcome,
            DocumentType documentType,
            DocumentProcessingOperation processingOperation,
            ProcessingOutcome processingOutcome,
            DocumentProcessingFailureReason processingFailureReason,
            Long durationMs,
            String processorVersion,
            String modelName,
            String promptVersion,
            ApplicationPage page) {
        this.id = UUID.randomUUID();
        this.sourceEventId = Objects.requireNonNull(sourceEventId, "Source event ID must not be null");
        this.category = Objects.requireNonNull(category, "Operational event category must not be null");
        this.eventName = Objects.requireNonNull(eventName, "Operational event name must not be null");
        this.userId = userId;
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurrence timestamp must not be null");
        this.patientResourceType = patientResourceType;
        this.patientAction = patientAction;
        this.authenticationAction = authenticationAction;
        this.authenticationOutcome = authenticationOutcome;
        this.documentType = documentType;
        this.processingOperation = processingOperation;
        this.processingOutcome = processingOutcome;
        this.processingFailureReason = processingFailureReason;
        this.durationMs = durationMs;
        this.processorVersion = processorVersion;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.page = page;
    }
}