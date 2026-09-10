package net.imaginethinking.appointmentpack.event.processing;

/**
 * Lists the supported values for document processing failure reason.
 */
public enum DocumentProcessingFailureReason {
    TIMEOUT,
    SERVICE_UNAVAILABLE,
    PROCESSING_ERROR,
    UNKNOWN
}