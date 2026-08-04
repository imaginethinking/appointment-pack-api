package net.imaginethinking.appointmentpack.document.processing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.document.Document;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "document_processing_results")
public class DocumentProcessingResult extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
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
            name = "generated_summary",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String generatedSummary;

    @Column(
            name = "reviewed_summary",
            columnDefinition = "TEXT"
    )
    private String reviewedSummary;

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
            name = "model_revision",
            length = 255
    )
    private String modelRevision;
}