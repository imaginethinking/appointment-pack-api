package net.imaginethinking.appointmentpack.document;

public enum DocumentStatus {
    UPLOADED,
    EXTRACTING,
    READY_FOR_DEIDENTIFICATION_REVIEW,
    SUMMARISING,
    READY_FOR_SUMMARY_REVIEW,
    EXTRACTION_FAILED,
    SUMMARISATION_FAILED,
    ACCEPTED,
    REJECTED,
    ARCHIVED
}
