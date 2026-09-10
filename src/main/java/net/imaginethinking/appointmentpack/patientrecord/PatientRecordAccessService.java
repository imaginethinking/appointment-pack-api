package net.imaginethinking.appointmentpack.patientrecord;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.patientcareraccess.PatientAccessControlService;
import net.imaginethinking.appointmentpack.permission.Permission;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Loads patient records and applies the patient access checks used by patient scoped features.
 */
@Service
@RequiredArgsConstructor
public class PatientRecordAccessService {

    private final PatientRecordRepository patientRecordRepository;
    private final PatientAccessControlService patientAccessControlService;

    /**
     * Loads the patient record or returns not found when the supplied ID does not exist.
     */
    @Transactional(readOnly = true)
    public PatientRecord requirePatientRecord(UUID patientRecordId) {
        return patientRecordRepository.findById(patientRecordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient record not found"));
    }

    /**
     * Loads the patient record and checks that the current user has the requested permission.
     *
     * @param authenticatedUserId user requesting access
     * @param patientRecordId patient record being accessed
     * @param requiredPermission permission needed for the action
     * @return the patient record after the access check succeeds
     */
    @Transactional(readOnly = true)
    public PatientRecord requireAccess(UUID authenticatedUserId, UUID patientRecordId, Permission requiredPermission) {
        PatientRecord patientRecord = requirePatientRecord(patientRecordId);

        return requireAccess(authenticatedUserId, patientRecord, requiredPermission);
    }

    /**
     * Loads the patient record and checks that the current user has the requested permission.
     */
    @Transactional(readOnly = true)
    public PatientRecord requireAccess(
            UUID authenticatedUserId,
            PatientRecord patientRecord,
            Permission requiredPermission) {
        patientAccessControlService.requirePermission(authenticatedUserId, patientRecord, requiredPermission);

        return patientRecord;
    }
}