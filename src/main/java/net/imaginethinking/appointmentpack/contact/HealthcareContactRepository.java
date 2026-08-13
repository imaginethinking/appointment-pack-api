package net.imaginethinking.appointmentpack.contact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HealthcareContactRepository extends JpaRepository<HealthcareContact, UUID> {

    List<HealthcareContact> findAllByPatientRecord_IdAndArchivedAtIsNullOrderByNameAsc(UUID patientRecordId);
}