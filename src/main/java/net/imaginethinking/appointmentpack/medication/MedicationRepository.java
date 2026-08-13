package net.imaginethinking.appointmentpack.medication;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MedicationRepository extends JpaRepository<Medication, UUID> {

    List<Medication> findAllByPatientRecord_IdAndArchivedAtIsNullOrderByStartDateDescCreatedAtDesc(UUID patientRecordId);
}