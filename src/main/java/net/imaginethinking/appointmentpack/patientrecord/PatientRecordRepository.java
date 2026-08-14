package net.imaginethinking.appointmentpack.patientrecord;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PatientRecordRepository extends JpaRepository<PatientRecord, UUID> {

    Optional<PatientRecord> findByProfileUserId(UUID userId);
    boolean existsByProfileId(UUID profileId);

}
