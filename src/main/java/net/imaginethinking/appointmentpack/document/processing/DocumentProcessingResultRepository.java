package net.imaginethinking.appointmentpack.document.processing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Defines the database queries used for document processing results.
 */
public interface DocumentProcessingResultRepository extends JpaRepository<DocumentProcessingResult, UUID> {
    /**
     * Loads the matching document processing result when it exists.
     */
    Optional<DocumentProcessingResult> findByDocumentId(UUID documentId);
}
