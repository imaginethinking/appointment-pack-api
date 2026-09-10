package net.imaginethinking.appointmentpack.audit;

import java.util.List;

/**
 * Represents patient audit page information returned by the API.
 */
public record PatientAuditPageResponse(
        List<PatientAuditResponse> events,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * Copies the activity list so the returned page cannot be changed after it is created.
     */
    public PatientAuditPageResponse {
        events = List.copyOf(events);
    }
}