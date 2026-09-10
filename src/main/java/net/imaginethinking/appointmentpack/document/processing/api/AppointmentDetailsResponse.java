package net.imaginethinking.appointmentpack.document.processing.api;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents appointment details information returned by the API.
 */
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

    /**
     * Keeps the address fields suggested by appointment document processing.
     */
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