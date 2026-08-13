package net.imaginethinking.appointmentpack.document.processing.client;

import java.util.UUID;

public record DocumentSummaryResponse(
        UUID documentId,
        String summary,
        String processorVersion,
        String modelName,
        String promptVersion
) {
}
