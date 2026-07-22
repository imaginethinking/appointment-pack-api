package net.imaginethinking.appointmentpack.patientcareraccess;

import net.imaginethinking.appointmentpack.profile.Profile;
import net.imaginethinking.appointmentpack.user.User;

import java.util.UUID;

public record CarerAccessSummaryResponse(
        UUID userId,
        UUID profileId,
        String firstName,
        String lastName,
        String email
) {

    public static CarerAccessSummaryResponse from(User user) {
        Profile profile = user.getProfile();

        return new CarerAccessSummaryResponse(
                user.getId(),
                profile.getId(),
                profile.getFirstName(),
                profile.getLastName(),
                user.getEmail()
        );
    }
}
