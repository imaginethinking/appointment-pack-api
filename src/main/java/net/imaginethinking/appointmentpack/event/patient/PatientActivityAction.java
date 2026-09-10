package net.imaginethinking.appointmentpack.event.patient;

/**
 * Lists the supported values for patient activity action.
 */
public enum PatientActivityAction {
    CREATED,
    UPDATED,
    ARCHIVED,
    UPLOADED,
    DOWNLOADED,
    GENERATED,
    INVITED,
    ACCEPTED,
    DECLINED,
    CANCELLED,
    REVOKED,
    PERMISSIONS_UPDATED,
    APPOINTMENT_CONFIRMED,
    APPOINTMENT_REJECTED,
    DEIDENTIFICATION_APPROVED,
    SUMMARY_ACCEPTED,
    SUMMARY_REJECTED
}