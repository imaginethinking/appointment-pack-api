package net.imaginethinking.appointmentpack.analytics.admin;

import java.util.List;

/**
 * Represents operational event page information returned by the API.
 */
public record OperationalEventPageResponse(
        List<OperationalEventResponse> events,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * Copies the event list so a returned analytics page cannot be changed after it is created.
     */
    public OperationalEventPageResponse {
        events = List.copyOf(events);
    }
}