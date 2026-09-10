package net.imaginethinking.appointmentpack.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Defines the database queries used for patient audit events.
 */
public interface PatientAuditEventRepository
        extends JpaRepository<PatientAuditEvent, UUID> {

    /**
     * Loads the matching patient audit events.
     */
    Page<PatientAuditEvent> findAllByPatientRecordIdOrderByOccurredAtDesc(UUID patientRecordId, Pageable pageable);
}