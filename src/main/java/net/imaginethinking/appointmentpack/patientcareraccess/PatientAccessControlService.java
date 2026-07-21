package net.imaginethinking.appointmentpack.patientcareraccess;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordPermission;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientAccessControlService {
    private final PatientCarerAccessRepository patientCarerAccessRepository;

    @Transactional(readOnly = true)
    public void requirePermission(UUID authenticatedUserId, PatientRecord patientRecord, PatientRecordPermission requiredPermission) {
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

    private boolean isOwner(UUID authenticatedUserId, PatientRecord patientRecord) {
        UUID ownerUserId = patientRecord.getProfile().getUser().getId();

        return ownerUserId.equals(authenticatedUserId);
    }

    private ResponseStatusException accessDenied() {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this patient record"
        );
    }

}
