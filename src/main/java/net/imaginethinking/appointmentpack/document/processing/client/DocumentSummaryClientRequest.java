package net.imaginethinking.appointmentpack.document.processing.client;

import java.util.UUID;

public record DocumentSummaryClientRequest(
        UUID documentId,
        String approvedDeidentifiedText
) {
}
