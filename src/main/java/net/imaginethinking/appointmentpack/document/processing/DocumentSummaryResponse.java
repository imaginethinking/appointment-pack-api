package net.imaginethinking.appointmentpack.document.processing;

import java.util.UUID;

public record DocumentSummaryResponse(
        UUID documentId,
        String summary,
        String processorVersion,
        String modelName,
        String promptVersion
) {
}
