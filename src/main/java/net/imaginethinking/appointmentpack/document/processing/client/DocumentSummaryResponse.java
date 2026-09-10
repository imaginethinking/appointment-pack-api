package net.imaginethinking.appointmentpack.document.processing.client;

import java.util.UUID;

/**
 * Represents document summary information returned by the API.
 */
public record DocumentSummaryResponse(
        UUID documentId,
        String summary,
        String processorVersion,
        String modelName,
        String promptVersion
) {
}
