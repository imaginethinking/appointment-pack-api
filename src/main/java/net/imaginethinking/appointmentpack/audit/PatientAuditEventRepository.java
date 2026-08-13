package net.imaginethinking.appointmentpack.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PatientAuditEventRepository
        extends JpaRepository<PatientAuditEvent, UUID> {

    Page<PatientAuditEvent> findAllByPatientRecordIdOrderByOccurredAtDesc(UUID patientRecordId, Pageable pageable);
}