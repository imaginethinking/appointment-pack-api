package net.imaginethinking.appointmentpack.document.processing.api;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentDetailsResponse(
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String service,
        String appointmentType,
        String clinicianOrTeam,
        String locationName,
        AddressDetails address
) {

    public record AddressDetails(
            String addressLine1,
            String addressLine2,
            String townCity,
            String county,
            String postcode,
            String country
    ) {
    }
}