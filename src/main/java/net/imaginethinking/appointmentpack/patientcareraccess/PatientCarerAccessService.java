package net.imaginethinking.appointmentpack.patientcareraccess;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordRepository;
import net.imaginethinking.appointmentpack.permission.PermissionValidator;
import net.imaginethinking.appointmentpack.user.EmailAddressNormalizer;
import net.imaginethinking.appointmentpack.user.User;
import net.imaginethinking.appointmentpack.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages carer invitations, relationship states and the permissions granted for each patient record.
 */
@Service
@RequiredArgsConstructor
public class PatientCarerAccessService {

    private final PatientCarerAccessRepository patientCarerAccessRepository;
    private final PatientRecordRepository patientRecordRepository;
    private final UserRepository userRepository;
    private final PermissionValidator permissionValidator;
    private final AppEventPublisher appEventPublisher;

    /**
     * Loads the owned patient record, validates the carer and permissions, then creates or reopens the
     * relationship as pending.
     */
    @Transactional
    public PatientCarerAccessResponse createInvitation(UUID patientUserId, CreateCarerInvitationRequest request) {
        PatientRecord patientRecord = requireOwnedPatientRecord(patientUserId);

        String carerEmail = EmailAddressNormalizer.normalise(request.carerEmail());

        User carer = userRepository.findByEmail(carerEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No registered account was found for this email address"));

        if (carer.getId().equals(patientUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot invite yourself as a carer");
        }

        Set<String> permissions = permissionValidator.validate(request.permissions());

        PatientCarerAccess access = patientCarerAccessRepository.findByPatientRecord_IdAndCarer_Id(
                        patientRecord.getId(),
                        carer.getId())
                .map(existingAccess -> prepareExistingInvitation(existingAccess, permissions))
                .orElseGet(() -> createNewInvitation(patientRecord, carer, permissions));

        PatientCarerAccess savedAccess = patientCarerAccessRepository.save(access);

        publishActivity(patientUserId, savedAccess, PatientActivityAction.INVITED);

        return PatientCarerAccessResponse.from(savedAccess);
    }

    /**
     * Loads a patient and carer relationship and returns it only when the current user is one of its participants.
     */
    @Transactional(readOnly = true)
    public PatientCarerAccessResponse getRelationship(UUID authenticatedUserId, UUID accessId) {
        PatientCarerAccess access = findAccess(accessId);

        requireParticipant(access, authenticatedUserId);

        return PatientCarerAccessResponse.from(access);
    }

    /**
     * Loads the relationship history for the patient record owned by the current user with the newest invitations
     * first.
     */
    @Transactional(readOnly = true)
    public List<PatientCarerAccessResponse> getRelationshipsAsPatient(UUID patientUserId) {
        PatientRecord patientRecord = requireOwnedPatientRecord(patientUserId);

        return patientCarerAccessRepository.findAllByPatientRecord_IdOrderByInvitedAtDesc(patientRecord.getId())
                .stream()
                .map(PatientCarerAccessResponse::from)
                .toList();
    }

    /**
     * Loads the relationship history where the current user is the carer with the newest invitations first.
     */
    @Transactional(readOnly = true)
    public List<PatientCarerAccessResponse> getRelationshipsAsCarer(UUID carerUserId) {
        return patientCarerAccessRepository.findAllByCarer_IdOrderByInvitedAtDesc(carerUserId)
                .stream()
                .map(PatientCarerAccessResponse::from)
                .toList();
    }

    /**
     * Checks that the current user is the invited carer and moves a pending relationship to active.
     */
    @Transactional
    public PatientCarerAccessResponse acceptInvitation(UUID carerUserId, UUID accessId) {
        PatientCarerAccess access = findAccess(accessId);

        requireCarer(access, carerUserId);
        requireStatus(access, PatientCarerAccessStatus.PENDING);

        changeStatus(access, PatientCarerAccessStatus.ACTIVE);

        publishActivity(carerUserId, access, PatientActivityAction.ACCEPTED);

        return PatientCarerAccessResponse.from(access);
    }

    /**
     * Checks that the current user is the invited carer and moves a pending relationship to declined.
     */
    @Transactional
    public PatientCarerAccessResponse declineInvitation(UUID carerUserId, UUID accessId) {
        PatientCarerAccess access = findAccess(accessId);

        requireCarer(access, carerUserId);
        requireStatus(access, PatientCarerAccessStatus.PENDING);

        changeStatus(access, PatientCarerAccessStatus.DECLINED);

        publishActivity(carerUserId, access, PatientActivityAction.DECLINED);

        return PatientCarerAccessResponse.from(access);
    }

    /**
     * Checks that the current user owns the patient record and cancels a pending invitation.
     */
    @Transactional
    public PatientCarerAccessResponse cancelInvitation(UUID patientUserId, UUID accessId) {
        PatientCarerAccess access = findAccess(accessId);

        requirePatientOwner(access, patientUserId);
        requireStatus(access, PatientCarerAccessStatus.PENDING);

        changeStatus(access, PatientCarerAccessStatus.CANCELLED);

        publishActivity(patientUserId, access, PatientActivityAction.CANCELLED);

        return PatientCarerAccessResponse.from(access);
    }

    /**
     * Checks that the current user owns the patient record and revokes an active carer relationship.
     */
    @Transactional
    public PatientCarerAccessResponse revokeAccess(UUID patientUserId, UUID accessId) {
        PatientCarerAccess access = findAccess(accessId);

        requirePatientOwner(access, patientUserId);
        requireStatus(access, PatientCarerAccessStatus.ACTIVE);

        changeStatus(access, PatientCarerAccessStatus.REVOKED);

        publishActivity(patientUserId, access, PatientActivityAction.REVOKED);

        return PatientCarerAccessResponse.from(access);
    }

    /**
     * Checks patient ownership, validates the new permission set and saves it when the relationship is pending or
     * active.
     */
    @Transactional
    public PatientCarerAccessResponse updatePermissions(
            UUID patientUserId,
            UUID accessId,
            UpdatePatientCarerPermissionsRequest request) {
        PatientCarerAccess access = findAccess(accessId);

        requirePatientOwner(access, patientUserId);

        if (access.getStatus() != PatientCarerAccessStatus.PENDING && access.getStatus() != PatientCarerAccessStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Permissions can only be updated for pending or active relationships");
        }

        Set<String> permissions = permissionValidator.validate(request.permissions());

        if (!access.getPermissions().equals(permissions)) {
            access.setPermissions(permissions);

            publishActivity(patientUserId, access, PatientActivityAction.PERMISSIONS_UPDATED);
        }

        return PatientCarerAccessResponse.from(access);
    }

    /**
     * Publishes the activity event for the completed change.
     */
    private void publishActivity(UUID authenticatedUserId, PatientCarerAccess access, PatientActivityAction action) {
        appEventPublisher.publish(PatientActivityEvent.create(
                authenticatedUserId,
                access.getPatientRecord().getId(),
                PatientResourceType.PATIENT_CARER_ACCESS,
                access.getId(),
                action));
    }

    /**
     * Creates a new pending relationship with the selected carer and permissions.
     */
    private PatientCarerAccess createNewInvitation(PatientRecord patientRecord, User carer, Set<String> permissions) {
        Instant now = Instant.now();

        PatientCarerAccess access = new PatientCarerAccess();

        access.setPatientRecord(patientRecord);
        access.setCarer(carer);
        access.setStatus(PatientCarerAccessStatus.PENDING);
        access.setPermissions(permissions);
        access.setInvitedAt(now);
        access.setStatusChangedAt(now);

        return access;
    }

    /**
     * Reopens a declined, revoked or cancelled relationship as a new pending invitation with updated permissions.
     */
    private PatientCarerAccess prepareExistingInvitation(PatientCarerAccess access, Set<String> permissions) {
        if (access.getStatus() == PatientCarerAccessStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending invitation already exists");
        }

        if (access.getStatus() == PatientCarerAccessStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This user is already an active carer");
        }

        Instant now = Instant.now();

        access.setStatus(PatientCarerAccessStatus.PENDING);
        access.setPermissions(permissions);
        access.setInvitedAt(now);
        access.setStatusChangedAt(now);

        return access;
    }

    /**
     * Loads the patient record owned by the current user or returns not found when they have not created one.
     */
    private PatientRecord requireOwnedPatientRecord(UUID patientUserId) {
        return patientRecordRepository.findByProfileUserId(patientUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient record not found"));
    }

    /**
     * Loads the patient and carer relationship or returns not found when it does not exist.
     */
    private PatientCarerAccess findAccess(UUID accessId) {
        return patientCarerAccessRepository.findById(accessId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient-carer relationship not found"));
    }

    /**
     * Allows the relationship to be viewed only by the patient owner or the assigned carer.
     */
    private void requireParticipant(PatientCarerAccess access, UUID authenticatedUserId) {
        if (isCarer(access, authenticatedUserId) || isPatientOwner(access, authenticatedUserId)) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view this carer relationship");
    }

    /**
     * Checks that the current user is the carer assigned to the relationship.
     */
    private void requireCarer(PatientCarerAccess access, UUID authenticatedUserId) {
        if (!isCarer(access, authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot respond to this invitation");
        }
    }

    /**
     * Checks that the current user owns the patient record linked to the relationship.
     */
    private void requirePatientOwner(PatientCarerAccess access, UUID authenticatedUserId) {
        if (!isPatientOwner(access, authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot manage this carer relationship");
        }
    }

    /**
     * Checks whether the current user is the carer assigned to the relationship.
     */
    private boolean isCarer(PatientCarerAccess access, UUID authenticatedUserId) {
        return access.getCarer().getId().equals(authenticatedUserId);
    }

    /**
     * Checks whether the current user owns the patient record linked to the relationship.
     */
    private boolean isPatientOwner(PatientCarerAccess access, UUID authenticatedUserId) {
        UUID patientOwnerId = access.getPatientRecord().getProfile().getUser().getId();

        return patientOwnerId.equals(authenticatedUserId);
    }

    /**
     * Checks that the relationship is in the state required for the requested change.
     */
    private void requireStatus(PatientCarerAccess access, PatientCarerAccessStatus expectedStatus) {
        if (access.getStatus() != expectedStatus) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The relationship is not in the required state");
        }
    }

    /**
     * Changes the relationship state and records when the state changed.
     */
    private void changeStatus(PatientCarerAccess access, PatientCarerAccessStatus status) {
        access.setStatus(status);
        access.setStatusChangedAt(Instant.now());
    }
}