package net.imaginethinking.appointmentpack.document;

/**
 * Lists the supported values for document status.
 */
public enum DocumentStatus {
    UPLOADED,
    EXTRACTING,
    READY_FOR_APPOINTMENT_REVIEW,
    READY_FOR_DEIDENTIFICATION_REVIEW,
    SUMMARISING,
    READY_FOR_SUMMARY_REVIEW,
    EXTRACTION_FAILED,
    SUMMARISATION_FAILED,
    ACCEPTED,
    REJECTED,
    ARCHIVED
}