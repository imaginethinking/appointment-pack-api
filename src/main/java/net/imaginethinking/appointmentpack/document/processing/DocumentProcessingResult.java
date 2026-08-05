package net.imaginethinking.appointmentpack.document.processing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.document.Document;
import net.imaginethinking.appointmentpack.user.User;

import java.time.Instant;

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
}