package net.imaginethinking.appointmentpack.appointment;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.address.AddressMapper;
import net.imaginethinking.appointmentpack.address.PartialAddressRequest;
import net.imaginethinking.appointmentpack.common.TextNormalizer;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
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
            CreateAppointmentRequest request) {
        PatientRecord patientRecord = patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecordId,
                AppointmentPermission.EDIT);

        validateTimes(request.startTime(), request.endTime());

        Appointment appointment = new Appointment();
        appointment.setPatientRecord(patientRecord);

        applyValues(
                appointment,
                request.date(),
                request.startTime(),
                request.endTime(),
                request.service(),
                request.appointmentType(),
                request.clinicianOrTeam(),
                request.locationName(),
                request.address(),
                request.notes());

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
            UpdateAppointmentRequest request) {
        Appointment appointment = findAvailableAppointment(appointmentId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                appointment.getPatientRecord(),
                AppointmentPermission.EDIT);

        validateTimes(request.startTime(), request.endTime());

        applyValues(
                appointment,
                request.date(),
                request.startTime(),
                request.endTime(),
                request.service(),
                request.appointmentType(),
                request.clinicianOrTeam(),
                request.locationName(),
                request.address(),
                request.notes());

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

    private void validateTimes(LocalTime startTime, LocalTime endTime) {
        if (endTime != null && !endTime.isAfter(startTime)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Appointment end time must be after the start time");
        }
    }

    private void applyValues(
            Appointment appointment,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String service,
            String appointmentType,
            String clinicianOrTeam,
            String locationName,
            PartialAddressRequest address,
            String notes) {
        appointment.setDate(date);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setService(TextNormalizer.stripToNull(service));
        appointment.setAppointmentType(TextNormalizer.stripToNull(appointmentType));
        appointment.setClinicianOrTeam(TextNormalizer.stripToNull(clinicianOrTeam));
        appointment.setLocationName(TextNormalizer.stripToNull(locationName));
        appointment.setAddress(AddressMapper.toAddress(address));
        appointment.setNotes(TextNormalizer.stripToNull(notes));
    }
}