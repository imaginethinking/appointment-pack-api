package net.imaginethinking.appointmentpack.document.processing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.document.Document;
import net.imaginethinking.appointmentpack.user.User;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Stores extracted text, review data and summary information produced while a document is processed.
 */
@Getter
@Setter
@Entity
@Table(name = "document_processing_results")
public class DocumentProcessingResult extends BaseEntity {

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "document_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_document_processing_result_document"
            )
    )
    private Document document;

    @Column(
            name = "extracted_text",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String extractedText;

    @Column(
            name = "machine_deidentified_text",
            columnDefinition = "TEXT"
    )
    private String machineDeidentifiedText;

    @Column(
            name = "approved_deidentified_text",
            columnDefinition = "TEXT"
    )
    private String approvedDeidentifiedText;

    @Column(name = "appointment_date")
    private LocalDate appointmentDate;

    @Column(name = "appointment_start_time")
    private LocalTime appointmentStartTime;

    @Column(name = "appointment_end_time")
    private LocalTime appointmentEndTime;

    @Column(
            name = "appointment_service",
            length = 250
    )
    private String appointmentService;

    @Column(
            name = "appointment_type",
            length = 250
    )
    private String appointmentType;

    @Column(
            name = "appointment_clinician_or_team",
            length = 250
    )
    private String appointmentClinicianOrTeam;

    @Column(
            name = "appointment_location_name",
            length = 250
    )
    private String appointmentLocationName;

    @Column(
            name = "appointment_address_line_1",
            length = 150
    )
    private String appointmentAddressLine1;

    @Column(
            name = "appointment_address_line_2",
            length = 150
    )
    private String appointmentAddressLine2;

    @Column(
            name = "appointment_town_city",
            length = 100
    )
    private String appointmentTownCity;

    @Column(
            name = "appointment_county",
            length = 100
    )
    private String appointmentCounty;

    @Column(
            name = "appointment_postcode",
            length = 20
    )
    private String appointmentPostcode;

    @Column(
            name = "appointment_country",
            length = 100
    )
    private String appointmentCountry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "appointment_reviewed_by_user_id",
            foreignKey = @ForeignKey(
                    name = "fk_processing_result_appointment_reviewer"
            )
    )
    private User appointmentReviewedBy;

    @Column(name = "appointment_reviewed_at")
    private Instant appointmentReviewedAt;

    @Column(
            name = "generated_summary",
            columnDefinition = "TEXT"
    )
    private String generatedSummary;

    @Column(
            name = "reviewed_summary",
            columnDefinition = "TEXT"
    )
    private String reviewedSummary;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "summary_source",
            length = 30
    )
    private SummarySource summarySource;

    @Column(
            name = "processing_warning",
            columnDefinition = "TEXT"
    )
    private String processingWarning;

    @Column(
            name = "processor_version",
            nullable = false,
            length = 100
    )
    private String processorVersion;

    @Column(
            name = "model_name",
            length = 255
    )
    private String modelName;

    @Column(
            name = "prompt_version",
            length = 100
    )
    private String promptVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "deidentification_reviewed_by_user_id",
            foreignKey = @ForeignKey(
                    name = "fk_processing_result_deidentification_reviewer"
            )
    )
    private User deidentificationReviewedBy;

    @Column(name = "deidentification_reviewed_at")
    private Instant deidentificationReviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "summary_reviewed_by_user_id",
            foreignKey = @ForeignKey(
                    name = "fk_processing_result_summary_reviewer"
            )
    )
    private User summaryReviewedBy;

    @Column(name = "summary_reviewed_at")
    private Instant summaryReviewedAt;
}