package net.imaginethinking.appointmentpack.appointment;

import net.imaginethinking.appointmentpack.address.AddressResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Represents appointment information returned by the API.
 */
public record AppointmentResponse(
        UUID id,
        UUID patientRecordId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String service,
        String appointmentType,
        String clinicianOrTeam,
        String locationName,
        AddressResponse address,
        String notes,
        UUID sourceDocumentId,
        Instant archivedAt
) {

    /**
     * Builds the appointment response from the supplied appointment.
     */
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientRecord().getId(),
                appointment.getDate(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getService(),
                appointment.getAppointmentType(),
                appointment.getClinicianOrTeam(),
                appointment.getLocationName(),
                AddressResponse.from(appointment.getAddress()),
                appointment.getNotes(),
                appointment.getSourceDocument() == null
                        ? null
                        : appointment.getSourceDocument().getId(),
                appointment.getArchivedAt()
        );
    }
}