package net.imaginethinking.appointmentpack.audit;

import java.util.List;

public record PatientAuditPageResponse(
        List<PatientAuditResponse> events,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public PatientAuditPageResponse {
        events = List.copyOf(events);
    }
}