package net.imaginethinking.appointmentpack.patientcareraccess;

import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.profile.Profile;

import java.util.UUID;

/**
 * Represents patient access summary information returned by the API.
 */
public record PatientAccessSummaryResponse(
        UUID patientRecordId,
        UUID userId,
        UUID profileId,
        String firstName,
        String lastName
) {

    /**
     * Builds the patient access summary response from the supplied patient record.
     */
    public static PatientAccessSummaryResponse from(PatientRecord patientRecord) {

        Profile profile = patientRecord.getProfile();

        return new PatientAccessSummaryResponse(
                patientRecord.getId(),
                profile.getUser().getId(),
                profile.getId(),
                profile.getFirstName(),
                profile.getLastName()
        );
    }
}
