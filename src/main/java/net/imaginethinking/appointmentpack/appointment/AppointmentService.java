package net.imaginethinking.appointmentpack.appointment;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.address.Address;
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

    @Transactional
    public AppointmentResponse createAppointment(
            UUID authenticatedUserId,
            UUID patientRecordId,
            AppointmentRequest request) {
        PatientRecord patientRecord = findPatientRecord(patientRecordId);

        patientAccessControlService.requirePermission(authenticatedUserId, patientRecord, AppointmentPermission.EDIT);

        validateTimes(request);

        Appointment appointment = new Appointment();

        appointment.setPatientRecord(patientRecord);

        applyValues(appointment, request);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        return AppointmentResponse.from(savedAppointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointments(UUID authenticatedUserId, UUID patientRecordId) {
        PatientRecord patientRecord = findPatientRecord(patientRecordId);

        patientAccessControlService.requirePermission(authenticatedUserId, patientRecord, AppointmentPermission.VIEW);

        return appointmentRepository.findAllByPatientRecord_IdAndArchivedAtIsNullOrderByDateAscStartTimeAsc(
                patientRecordId).stream().map(AppointmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(UUID authenticatedUserId, UUID appointmentId) {
        Appointment appointment = findAvailableAppointment(appointmentId);

        patientAccessControlService.requirePermission(
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

        patientAccessControlService.requirePermission(
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

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                appointment.getPatientRecord(),
                AppointmentPermission.EDIT);

        appointment.archive();

        return AppointmentResponse.from(appointment);
    }

    private PatientRecord findPatientRecord(UUID patientRecordId) {
        return patientRecordRepository.findById(patientRecordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient record not found"));
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

        appointment.setService(normaliseOptionalValue(request.service()));
        appointment.setAppointmentType(normaliseOptionalValue(request.appointmentType()));
        appointment.setClinicianOrTeam(normaliseOptionalValue(request.clinicianOrTeam()));
        appointment.setLocationName(normaliseOptionalValue(request.locationName()));
        appointment.setAddress(toAddress(request.address()));
        appointment.setNotes(normaliseOptionalValue(request.notes()));
    }

    private Address toAddress(
            AppointmentRequest.AddressInput request) {
        if (request == null) {
            return null;
        }

        boolean empty = isBlank(request.addressLine1()) && isBlank(request.addressLine2()) && isBlank(request.townCity()) && isBlank(
                request.county()) && isBlank(request.postcode()) && isBlank(request.country());

        if (empty) {
            return null;
        }

        Address address = new Address();

        address.setAddressLine1(normaliseOptionalValue(request.addressLine1()));
        address.setAddressLine2(normaliseOptionalValue(request.addressLine2()));
        address.setTownCity(normaliseOptionalValue(request.townCity()));
        address.setCounty(normaliseOptionalValue(request.county()));
        address.setPostcode(normaliseOptionalValue(request.postcode()));
        address.setCountry(normaliseOptionalValue(request.country()));

        return address;
    }

    private String normaliseOptionalValue(String value) {
        if (isBlank(value)) {
            return null;
        }

        return value.strip();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}