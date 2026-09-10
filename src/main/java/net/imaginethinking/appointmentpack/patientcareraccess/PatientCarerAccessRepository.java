package net.imaginethinking.appointmentpack.patientcareraccess;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines the database queries used for patient and carer relationships.
 */
public interface PatientCarerAccessRepository extends JpaRepository<PatientCarerAccess, UUID> {

    /**
     * Loads the matching patient and carer relationship when it exists.
     */
    Optional<PatientCarerAccess> findByPatientRecord_IdAndCarer_Id(
            UUID patientRecordId,
            UUID carerUserId
    );

    /**
     * Loads the matching patient and carer relationship when it exists.
     */
    @Override
    @EntityGraph(attributePaths = {
            "patientRecord",
            "patientRecord.profile",
            "patientRecord.profile.user",
            "carer",
            "carer.profile",
            "permissions"
    })
    Optional<PatientCarerAccess> findById(UUID accessId);

    /**
     * Loads the matching patient and carer relationships.
     */
    @EntityGraph(attributePaths = {
            "patientRecord",
            "patientRecord.profile",
            "patientRecord.profile.user",
            "carer",
            "carer.profile",
            "permissions"
    })
    List<PatientCarerAccess> findAllByPatientRecord_IdOrderByInvitedAtDesc(UUID patientRecordId);

    /**
     * Loads the matching patient and carer relationships.
     */
    @EntityGraph(attributePaths = {
            "patientRecord",
            "patientRecord.profile",
            "patientRecord.profile.user",
            "carer",
            "carer.profile",
            "permissions"
    })
    List<PatientCarerAccess> findAllByCarer_IdOrderByInvitedAtDesc(UUID carerUserId);

    /**
     * Loads the matching patient and carer relationship when it exists.
     */
    @EntityGraph(attributePaths = "permissions")
    Optional<PatientCarerAccess> findByPatientRecordIdAndCarerIdAndStatus(
            UUID patientRecordId,
            UUID carerId,
            PatientCarerAccessStatus status
    );
}