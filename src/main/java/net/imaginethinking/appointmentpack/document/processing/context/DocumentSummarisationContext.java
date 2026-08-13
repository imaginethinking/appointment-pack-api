package net.imaginethinking.appointmentpack.document.processing.context;

import java.util.UUID;

public record DocumentSummarisationContext(
        UUID documentId,
        String approvedDeidentifiedText
) {
}
