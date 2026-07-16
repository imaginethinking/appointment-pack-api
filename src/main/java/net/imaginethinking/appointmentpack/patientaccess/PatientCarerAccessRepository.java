package net.imaginethinking.appointmentpack.patientaccess;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientCarerAccessRepository extends JpaRepository<PatientCarerAccess, UUID> {

    Optional<PatientCarerAccess> findByPatientRecord_IdAndCarer_Id(UUID patientRecordId, UUID carerUserId);

    @EntityGraph(attributePaths = {
            "patientRecord",
            "patientRecord.profile",
            "patientRecord.profile.user",
            "carer",
            "carer.profile"
    })
    List<PatientCarerAccess> findAllByPatientRecord_IdOrderByInvitedAtDesc(UUID patientRecordId);

    @EntityGraph(attributePaths = {
            "patientRecord",
            "patientRecord.profile",
            "patientRecord.profile.user",
            "carer",
            "carer.profile"
    })
    List<PatientCarerAccess> findAllByCarer_IdOrderByInvitedAtDesc(UUID carerUserId);
}
