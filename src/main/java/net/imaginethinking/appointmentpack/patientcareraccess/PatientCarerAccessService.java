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

@Service
@RequiredArgsConstructor
public class PatientCarerAccessService {

    private final PatientCarerAccessRepository patientCarerAccessRepository;
    private final PatientRecordRepository patientRecordRepository;
    private final UserRepository userRepository;
    private final PermissionValidator permissionValidator;
    private final AppEventPublisher appEventPublisher;

    @Transactional
    public PatientCarerAccessResponse createInvitation(UUID patientUserId, CreateCarerInvitationRequest request) {
        PatientRecord patientRecord = requireOwnedPatientRecord(patientUserId);

        String carerEmail = EmailAddressNormalizer.normalise(request.carerEmail());

        User carer = userRepository.findByEmail(carerEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No registered account was found for this email address")
                );

        if (carer.getId().equals(patientUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot invite yourself as a carer"
            );
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

    @Transactional(readOnly = true)
    public PatientCarerAccessResponse getRelationship(UUID authenticatedUserId, UUID accessId) {
        PatientCarerAccess access = findAccess(accessId);

        requireParticipant(access, authenticatedUserId);

        return PatientCarerAccessResponse.from(access);
    }

    @Transactional(readOnly = true)
    public List<PatientCarerAccessResponse> getRelationshipsAsPatient(UUID patientUserId) {
        PatientRecord patientRecord = requireOwnedPatientRecord(patientUserId);

        return patientCarerAccessRepository.findAllByPatientRecord_IdOrderByInvitedAtDesc(patientRecord.getId())
                .stream()
                .map(PatientCarerAccessResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PatientCarerAccessResponse> getRelationshipsAsCarer(UUID carerUserId) {
        return patientCarerAccessRepository.findAllByCarer_IdOrderByInvitedAtDesc(carerUserId)
                .stream()
                .map(PatientCarerAccessResponse::from)
                .toList();
    }

    @Transactional
    public PatientCarerAccessResponse acceptInvitation(UUID carerUserId, UUID accessId) {
        PatientCarerAccess access = findAccess(accessId);

        requireCarer(access, carerUserId);
        requireStatus(access, PatientCarerAccessStatus.PENDING);

        changeStatus(access, PatientCarerAccessStatus.ACTIVE);

        publishActivity(carerUserId, access, PatientActivityAction.ACCEPTED);

        return PatientCarerAccessResponse.from(access);
    }

    @Transactional
    public PatientCarerAccessResponse declineInvitation(UUID carerUserId, UUID accessId) {
        PatientCarerAccess access = findAccess(accessId);

        requireCarer(access, carerUserId);
        requireStatus(access, PatientCarerAccessStatus.PENDING);

        changeStatus(access, PatientCarerAccessStatus.DECLINED);

        publishActivity(carerUserId, access, PatientActivityAction.DECLINED);

        return PatientCarerAccessResponse.from(access);
    }

    @Transactional
    public PatientCarerAccessResponse cancelInvitation(UUID patientUserId, UUID accessId) {
        PatientCarerAccess access = findAccess(accessId);

        requirePatientOwner(access, patientUserId);
        requireStatus(access, PatientCarerAccessStatus.PENDING);

        changeStatus(access, PatientCarerAccessStatus.CANCELLED);

        publishActivity(patientUserId, access, PatientActivityAction.CANCELLED);

        return PatientCarerAccessResponse.from(access);
    }

    @Transactional
    public PatientCarerAccessResponse revokeAccess(UUID patientUserId, UUID accessId) {
        PatientCarerAccess access = findAccess(accessId);

        requirePatientOwner(access, patientUserId);
        requireStatus(access, PatientCarerAccessStatus.ACTIVE);

        changeStatus(access, PatientCarerAccessStatus.REVOKED);

        publishActivity(patientUserId, access, PatientActivityAction.REVOKED);

        return PatientCarerAccessResponse.from(access);
    }

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

        access.setPermissions(permissions);
        access.setStatusChangedAt(Instant.now());

        publishActivity(patientUserId, access, PatientActivityAction.PERMISSIONS_UPDATED);

        return PatientCarerAccessResponse.from(access);
    }

    private void publishActivity(UUID authenticatedUserId, PatientCarerAccess access, PatientActivityAction action) {
        appEventPublisher.publish(PatientActivityEvent.create(
                authenticatedUserId,
                access.getPatientRecord().getId(),
                PatientResourceType.PATIENT_CARER_ACCESS,
                access.getId(),
                action));
    }

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

    private PatientCarerAccess prepareExistingInvitation(PatientCarerAccess access, Set<String> permissions) {
        if (access.getStatus() == PatientCarerAccessStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A pending invitation already exists"
            );
        }

        if (access.getStatus() == PatientCarerAccessStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This user is already an active carer"
            );
        }

        Instant now = Instant.now();

        access.setStatus(PatientCarerAccessStatus.PENDING);
        access.setPermissions(permissions);
        access.setInvitedAt(now);
        access.setStatusChangedAt(now);

        return access;
    }

    private PatientRecord requireOwnedPatientRecord(UUID patientUserId) {
        return patientRecordRepository.findByProfileUserId(patientUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient record not found")
                );
    }

    private PatientCarerAccess findAccess(UUID accessId) {
        return patientCarerAccessRepository.findById(accessId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient-carer relationship not found")
                );
    }

    private void requireParticipant(PatientCarerAccess access, UUID authenticatedUserId) {
        if (isCarer(access, authenticatedUserId) || isPatientOwner(access, authenticatedUserId)) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You cannot view this carer relationship"
        );
    }

    private void requireCarer(PatientCarerAccess access, UUID authenticatedUserId) {
        if (!isCarer(access, authenticatedUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot respond to this invitation"
            );
        }
    }

    private void requirePatientOwner(PatientCarerAccess access, UUID authenticatedUserId) {
        if (!isPatientOwner(access, authenticatedUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot manage this carer relationship"
            );
        }
    }

    private boolean isCarer(PatientCarerAccess access, UUID authenticatedUserId) {
        return access.getCarer().getId().equals(authenticatedUserId);
    }

    private boolean isPatientOwner(PatientCarerAccess access, UUID authenticatedUserId) {
        UUID patientOwnerId = access.getPatientRecord().getProfile().getUser().getId();

        return patientOwnerId.equals(authenticatedUserId);
    }

    private void requireStatus(PatientCarerAccess access, PatientCarerAccessStatus expectedStatus) {
        if (access.getStatus() != expectedStatus) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The relationship is not in the required state"
            );
        }
    }

    private void changeStatus(PatientCarerAccess access, PatientCarerAccessStatus status) {
        access.setStatus(status);
        access.setStatusChangedAt(Instant.now());
    }
}