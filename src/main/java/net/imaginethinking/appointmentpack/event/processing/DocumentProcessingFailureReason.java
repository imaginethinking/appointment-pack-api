package net.imaginethinking.appointmentpack.event.processing;

public enum DocumentProcessingFailureReason {
    TIMEOUT,
    SERVICE_UNAVAILABLE,
    PROCESSING_ERROR,
    UNKNOWN
}