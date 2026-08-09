package net.imaginethinking.appointmentpack.appointment;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.patientcareraccess.PatientAccessControlService;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRecordRepository patientRecordRepository;
    private final PatientAccessControlService patientAccessControlService;

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointments(UUID authenticatedUserId, UUID patientRecordId) {
        PatientRecord patientRecord = patientRecordRepository.findById(patientRecordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient record not found"));

        patientAccessControlService.requirePermission(authenticatedUserId, patientRecord, AppointmentPermission.VIEW);

        return appointmentRepository.findAllByPatientRecord_IdOrderByDateAscStartTimeAsc(patientRecordId)
                .stream()
                .map(AppointmentResponse::from)
                .toList();
    }
}