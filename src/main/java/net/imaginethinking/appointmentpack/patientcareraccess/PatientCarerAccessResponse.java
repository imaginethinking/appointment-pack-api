package net.imaginethinking.appointmentpack.patientcareraccess;

import net.imaginethinking.appointmentpack.profile.Profile;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record PatientCarerAccessResponse(
        UUID id,
        UUID patientRecordId,
        String patientName,
        UUID carerUserId,
        String carerName,
        Set<String> permissions,
        PatientCarerAccessStatus status,
        Instant invitedAt,
        Instant statusChangedAt
) {

    public static PatientCarerAccessResponse from(PatientCarerAccess access) {
        Profile patientProfile = access.getPatientRecord().getProfile();
        Profile carerProfile = access.getCarer().getProfile();

        return new PatientCarerAccessResponse(
                access.getId(),
                access.getPatientRecord().getId(),
                fullName(patientProfile),
                access.getCarer().getId(),
                fullName(carerProfile),
                Set.copyOf(access.getPermissions()),
                access.getStatus(),
                access.getInvitedAt(),
                access.getStatusChangedAt()
        );
    }

    private static String fullName(Profile profile) {
        return profile.getFirstName() + " " + profile.getLastName();
    }
}
