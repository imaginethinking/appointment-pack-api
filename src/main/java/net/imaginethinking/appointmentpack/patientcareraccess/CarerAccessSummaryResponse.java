package net.imaginethinking.appointmentpack.patientcareraccess;

import net.imaginethinking.appointmentpack.profile.Profile;
import net.imaginethinking.appointmentpack.user.User;

import java.util.UUID;

/**
 * Represents carer access summary information returned by the API.
 */
public record CarerAccessSummaryResponse(
        UUID userId,
        UUID profileId,
        String firstName,
        String lastName,
        String email
) {

    /**
     * Builds the carer access summary response from the supplied user.
     */
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
