package net.imaginethinking.appointmentpack.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findAllByPatientRecordIdAndStatusNotOrderByCreatedAtDesc(
            UUID patientRecordId,
            DocumentStatus status
    );

    Optional<Document> findByIdAndPatientRecordId(
            UUID documentId,
            UUID patientRecordId
    );
}
