package net.imaginethinking.appointmentpack.document.processing;

import java.util.UUID;

public record DocumentSummarisationContext(
        UUID documentId,
        String approvedDeidentifiedText
) {
}
