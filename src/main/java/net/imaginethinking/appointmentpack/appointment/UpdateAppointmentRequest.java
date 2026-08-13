package net.imaginethinking.appointmentpack.appointment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.imaginethinking.appointmentpack.address.PartialAddressRequest;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateAppointmentRequest(

        @NotNull(message = "Appointment date is required")
        LocalDate date,

        @NotNull(message = "Appointment start time is required")
        LocalTime startTime,

        LocalTime endTime,

        @Size(
                max = 250,
                message = "Service must not exceed 250 characters"
        )
        String service,

        @Size(
                max = 250,
                message = "Appointment type must not exceed 250 characters"
        )
        String appointmentType,

        @Size(
                max = 250,
                message = "Clinician or team must not exceed 250 characters"
        )
        String clinicianOrTeam,

        @Size(
                max = 250,
                message = "Location name must not exceed 250 characters"
        )
        String locationName,

        @Valid
        PartialAddressRequest address,

        @Size(
                max = 2000,
                message = "Notes must not exceed 2000 characters"
        )
        String notes
) {
}