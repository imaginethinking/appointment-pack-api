package net.imaginethinking.appointmentpack.patientcareraccess;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.permission.Permission;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Allows patient owners directly and checks active carer relationships for any requested patient permission.
 */
@Service
@RequiredArgsConstructor
public class PatientAccessControlService {
    private final PatientCarerAccessRepository patientCarerAccessRepository;

    /**
     * Allows the patient owner directly, otherwise checks for an active carer relationship with the requested
     * permission.
     *
     * @param authenticatedUserId user requesting access
     * @param patientRecord patient record being accessed
     * @param requiredPermission permission needed for the action
     * @throws ResponseStatusException when the user does not have access to the patient record
     */
    @Transactional(readOnly = true)
    public void requirePermission(UUID authenticatedUserId, PatientRecord patientRecord, Permission requiredPermission) {
        if (isOwner(authenticatedUserId, patientRecord)) {
            return;
        }

        PatientCarerAccess access = patientCarerAccessRepository
                .findByPatientRecordIdAndCarerIdAndStatus(
                        patientRecord.getId(),
                        authenticatedUserId,
                        PatientCarerAccessStatus.ACTIVE
                )
                .orElseThrow(this::accessDenied);

        if (!access.getPermissions().contains(requiredPermission.value())) {
            throw accessDenied();
        }

    }

    /**
     * Checks whether the current user owns the selected patient record.
     */
    private boolean isOwner(UUID authenticatedUserId, PatientRecord patientRecord) {
        UUID ownerUserId = patientRecord.getProfile().getUser().getId();

        return ownerUserId.equals(authenticatedUserId);
    }

    /**
     * Creates the forbidden response used when patient access cannot be granted.
     */
    private ResponseStatusException accessDenied() {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this patient record"
        );
    }

}
