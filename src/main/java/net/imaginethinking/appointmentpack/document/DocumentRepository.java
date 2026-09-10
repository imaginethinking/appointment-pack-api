package net.imaginethinking.appointmentpack.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Defines the database queries used for documents.
 */
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /**
     * Loads the matching documents with the newest record first.
     */
    List<Document> findAllByPatientRecordIdAndStatusNotOrderByCreatedAtDesc(
            UUID patientRecordId,
            DocumentStatus status
    );

    /**
     * Updates matching documents that have remained in the processing state past the supplied cutoff.
     */
    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            update Document document
            set document.status = :failedStatus,
                document.processingFailureReason = :failureReason,
                document.updatedAt = :recoveredAt,
                document.version = document.version + 1
            where document.status = :processingStatus
              and document.updatedAt < :cutoff
            """)
    int recoverStaleProcessingState(
            @Param("processingStatus")
            DocumentStatus processingStatus,

            @Param("failedStatus")
            DocumentStatus failedStatus,

            @Param("cutoff")
            Instant cutoff,

            @Param("recoveredAt")
            Instant recoveredAt,

            @Param("failureReason")
            String failureReason
    );
}