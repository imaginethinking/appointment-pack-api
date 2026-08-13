package net.imaginethinking.appointmentpack.patientrecord;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.patientcareraccess.PatientAccessControlService;
import net.imaginethinking.appointmentpack.permission.Permission;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientRecordAccessService {

    private final PatientRecordRepository patientRecordRepository;
    private final PatientAccessControlService patientAccessControlService;

    @Transactional(readOnly = true)
    public PatientRecord requirePatientRecord(UUID patientRecordId) {
        return patientRecordRepository.findById(patientRecordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient record not found"));
    }

    @Transactional(readOnly = true)
    public PatientRecord requireAccess(UUID authenticatedUserId, UUID patientRecordId, Permission requiredPermission) {
        PatientRecord patientRecord = requirePatientRecord(patientRecordId);

        return requireAccess(authenticatedUserId, patientRecord, requiredPermission);
    }

    @Transactional(readOnly = true)
    public PatientRecord requireAccess(
            UUID authenticatedUserId,
            PatientRecord patientRecord,
            Permission requiredPermission) {
        patientAccessControlService.requirePermission(authenticatedUserId, patientRecord, requiredPermission);

        return patientRecord;
    }
}