package net.imaginethinking.appointmentpack.patientcareraccess;

import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.profile.Profile;

import java.util.UUID;

public record PatientAccessSummaryResponse(
        UUID patientRecordId,
        UUID userId,
        UUID profileId,
        String firstName,
        String lastName
) {

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
