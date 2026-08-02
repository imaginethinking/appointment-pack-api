package net.imaginethinking.appointmentpack.document.processing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentProcessingResultRepository extends JpaRepository<DocumentProcessingResult, UUID> {
    Optional<DocumentProcessingResult> findByDocumentId(UUID documentId);
}
