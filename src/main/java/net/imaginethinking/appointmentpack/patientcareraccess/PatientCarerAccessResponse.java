package net.imaginethinking.appointmentpack.patientcareraccess;

import net.imaginethinking.appointmentpack.profile.Profile;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Represents patient and carer relationship information returned by the API.
 */
public record PatientCarerAccessResponse(
        UUID id,
        PatientAccessSummaryResponse patient,
        CarerAccessSummaryResponse carer,
        PatientCarerAccessStatus status,
        Set<String> permissions,
        Instant invitedAt,
        Instant statusChangedAt
) {

    /**
     * Builds the relationship response from the saved patient, carer, status and permission details.
     */
    public static PatientCarerAccessResponse from(PatientCarerAccess access) {

        return new PatientCarerAccessResponse(
                access.getId(),
                PatientAccessSummaryResponse.from(
                        access.getPatientRecord()
                ),
                CarerAccessSummaryResponse.from(
                        access.getCarer()
                ),
                access.getStatus(),
                Set.copyOf(access.getPermissions()),
                access.getInvitedAt(),
                access.getStatusChangedAt()
        );
    }
}
