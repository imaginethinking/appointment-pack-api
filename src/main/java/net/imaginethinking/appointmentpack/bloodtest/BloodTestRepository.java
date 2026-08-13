package net.imaginethinking.appointmentpack.bloodtest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BloodTestRepository
        extends JpaRepository<BloodTest, UUID> {

    List<BloodTest> findAllByPatientRecord_IdAndArchivedAtIsNullOrderByTestDateDescCreatedAtDesc(UUID patientRecordId);
}