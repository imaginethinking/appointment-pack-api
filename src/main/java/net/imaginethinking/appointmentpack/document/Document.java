package net.imaginethinking.appointmentpack.document;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.user.User;

@Getter
@Setter
@Entity
@Table(
        name = "documents",
        indexes = {
                @Index(
                        name = "idx_documents_patient_record",
                        columnList = "patient_record_id"
                ),
                @Index(
                        name = "idx_documents_status",
                        columnList = "status"
                )
        }
)
public class Document extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "patient_record_id",
            nullable = false
    )
    private PatientRecord patientRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "uploaded_by_user_id",
            nullable = false
    )
    private User uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentStatus status = DocumentStatus.UPLOADED;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "stored_file_name", unique = true, length = 100, nullable = false)
    private String storedFileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "storage_path", unique = true, length = 500, nullable = false)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;
}
