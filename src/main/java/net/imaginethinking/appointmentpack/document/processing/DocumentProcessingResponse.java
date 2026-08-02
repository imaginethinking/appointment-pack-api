package net.imaginethinking.appointmentpack.document.processing;

import java.util.List;
import java.util.UUID;

public record DocumentProcessingResponse(
        UUID documentId,
        String extractedText,
        String summary,
        List<KeyPoint> keyPoints,
        List<String> warnings,
        String processorVersion,
        ModelMetadata model
) {

    public record KeyPoint(
            KeyPointType type,
            String text,
            Integer sourcePage
    ) {
    }

    public record ModelMetadata(
            String name,
            String revision
    ) {
    }

    public enum KeyPointType {
        APPOINTMENT,
        OUTCOME,
        FOLLOW_UP,
        MEDICATION,
        OTHER
    }
}