package net.imaginethinking.appointmentpack.document.processing;

public record DocumentProcessingKeyPoint(
        KeyPointType type,
        String text,
        Integer sourcePage
) {

    public enum KeyPointType {
        APPOINTMENT,
        OUTCOME,
        FOLLOW_UP,
        MEDICATION,
        OTHER
    }
}