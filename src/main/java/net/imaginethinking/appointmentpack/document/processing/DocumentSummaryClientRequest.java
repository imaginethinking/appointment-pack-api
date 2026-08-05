package net.imaginethinking.appointmentpack.document.processing;

import java.util.UUID;

public record DocumentSummaryClientRequest(
        UUID documentId,
        String approvedDeidentifiedText
) {
}
