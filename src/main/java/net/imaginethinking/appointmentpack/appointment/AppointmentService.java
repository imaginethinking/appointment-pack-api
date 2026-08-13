package net.imaginethinking.appointmentpack.appointment;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.address.AddressMapper;
import net.imaginethinking.appointmentpack.common.TextNormalizer;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
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
    private final PatientRecordAccessService patientRecordAccessService;

    @Transactional
    public AppointmentResponse createAppointment(
            UUID authenticatedUserId,
            UUID patientRecordId,
            AppointmentRequest request) {
        PatientRecord patientRecord = patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecordId,
                AppointmentPermission.EDIT);

        validateTimes(request);

        Appointment appointment = new Appointment();

        appointment.setPatientRecord(patientRecord);

        applyValues(appointment, request);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        return AppointmentResponse.from(savedAppointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointments(UUID authenticatedUserId, UUID patientRecordId) {
        patientRecordAccessService.requireAccess(authenticatedUserId, patientRecordId, AppointmentPermission.VIEW);

        return appointmentRepository.findAllByPatientRecord_IdAndArchivedAtIsNullOrderByDateAscStartTimeAsc(
                patientRecordId).stream().map(AppointmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(UUID authenticatedUserId, UUID appointmentId) {
        Appointment appointment = findAvailableAppointment(appointmentId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                appointment.getPatientRecord(),
                AppointmentPermission.VIEW);

        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse updateAppointment(
            UUID authenticatedUserId,
            UUID appointmentId,
            AppointmentRequest request) {
        Appointment appointment = findAvailableAppointment(appointmentId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                appointment.getPatientRecord(),
                AppointmentPermission.EDIT);

        validateTimes(request);

        applyValues(appointment, request);

        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse archiveAppointment(UUID authenticatedUserId, UUID appointmentId) {
        Appointment appointment = findAppointment(appointmentId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                appointment.getPatientRecord(),
                AppointmentPermission.EDIT);

        appointment.archive();

        return AppointmentResponse.from(appointment);
    }

    private Appointment findAppointment(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
    }

    private Appointment findAvailableAppointment(UUID appointmentId) {
        Appointment appointment = findAppointment(appointmentId);

        if (appointment.isArchived()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found");
        }

        return appointment;
    }

    private void validateTimes(AppointmentRequest request) {
        if (request.endTime() != null && !request.endTime().isAfter(request.startTime())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Appointment end time must be after the start time");
        }
    }

    private void applyValues(Appointment appointment, AppointmentRequest request) {
        appointment.setDate(request.date());
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(request.endTime());

        appointment.setService(TextNormalizer.stripToNull(request.service()));
        appointment.setAppointmentType(TextNormalizer.stripToNull(request.appointmentType()));
        appointment.setClinicianOrTeam(TextNormalizer.stripToNull(request.clinicianOrTeam()));
        appointment.setLocationName(TextNormalizer.stripToNull(request.locationName()));
        appointment.setAddress(AddressMapper.toAddress(request.address()));
        appointment.setNotes(TextNormalizer.stripToNull(request.notes()));
    }
}