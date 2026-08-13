package net.imaginethinking.appointmentpack.patientcareraccess;

import lombok.RequiredArgsConstructor;
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

    @Transactional
    public PatientCarerAccessResponse inviteCarer(UUID patientUserId, CreateCarerInvitationRequest request) {
        PatientRecord patientRecord = getOwnedPatientRecord(patientUserId);

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
                .orElseGet(() -> createInvitation(patientRecord, carer, permissions));

        PatientCarerAccess savedAccess = patientCarerAccessRepository.save(access);

        return PatientCarerAccessResponse.from(savedAccess);
    }

    @Transactional(readOnly = true)
    public List<PatientCarerAccessResponse> getRelationshipsAsPatient(UUID patientUserId) {
        PatientRecord patientRecord = getOwnedPatientRecord(patientUserId);

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
        PatientCarerAccess access = getAccess(accessId);

        requireCarer(access, carerUserId);
        requireStatus(access, PatientCarerAccessStatus.PENDING);

        changeStatus(access, PatientCarerAccessStatus.ACTIVE);

        return PatientCarerAccessResponse.from(access);
    }

    @Transactional
    public PatientCarerAccessResponse declineInvitation(UUID carerUserId, UUID accessId) {
        PatientCarerAccess access = getAccess(accessId);

        requireCarer(access, carerUserId);
        requireStatus(access, PatientCarerAccessStatus.PENDING);

        changeStatus(access, PatientCarerAccessStatus.DECLINED);

        return PatientCarerAccessResponse.from(access);
    }

    @Transactional
    public PatientCarerAccessResponse cancelInvitation(UUID patientUserId, UUID accessId) {
        PatientCarerAccess access = getAccess(accessId);

        requirePatientOwner(access, patientUserId);
        requireStatus(access, PatientCarerAccessStatus.PENDING);

        changeStatus(access, PatientCarerAccessStatus.CANCELLED);

        return PatientCarerAccessResponse.from(access);
    }

    @Transactional
    public PatientCarerAccessResponse revokeAccess(UUID patientUserId, UUID accessId) {
        PatientCarerAccess access = getAccess(accessId);

        requirePatientOwner(access, patientUserId);
        requireStatus(access, PatientCarerAccessStatus.ACTIVE);

        changeStatus(access, PatientCarerAccessStatus.REVOKED);

        return PatientCarerAccessResponse.from(access);
    }

    @Transactional
    public PatientCarerAccessResponse updatePermissions(
            UUID patientUserId,
            UUID accessId,
            UpdatePatientCarerPermissionsRequest request) {
        PatientCarerAccess access = getAccess(accessId);

        requirePatientOwner(access, patientUserId);

        if (access.getStatus() != PatientCarerAccessStatus.PENDING && access.getStatus() != PatientCarerAccessStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Permissions can only be updated for pending or active relationships");
        }

        Set<String> permissions = permissionValidator.validate(request.permissions());

        access.setPermissions(permissions);
        access.setStatusChangedAt(Instant.now());

        return PatientCarerAccessResponse.from(access);
    }

    private PatientCarerAccess createInvitation(PatientRecord patientRecord, User carer, Set<String> permissions) {
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

    private PatientRecord getOwnedPatientRecord(UUID patientUserId) {
        return patientRecordRepository.findByProfileUserId(patientUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient record not found")
                );
    }

    private PatientCarerAccess getAccess(UUID accessId) {
        return patientCarerAccessRepository.findById(accessId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient-carer relationship not found")
                );
    }

    private void requireCarer(PatientCarerAccess access, UUID authenticatedUserId) {
        if (!access.getCarer().getId().equals(authenticatedUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot respond to this invitation"
            );
        }
    }

    private void requirePatientOwner(PatientCarerAccess access, UUID authenticatedUserId) {
        UUID patientOwnerId = access.getPatientRecord().getProfile().getUser().getId();

        if (!patientOwnerId.equals(authenticatedUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot manage this carer relationship"
            );
        }
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