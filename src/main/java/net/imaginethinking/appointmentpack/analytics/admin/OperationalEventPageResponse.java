package net.imaginethinking.appointmentpack.analytics.admin;

import java.util.List;

public record OperationalEventPageResponse(
        List<OperationalEventResponse> events,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public OperationalEventPageResponse {
        events = List.copyOf(events);
    }
}