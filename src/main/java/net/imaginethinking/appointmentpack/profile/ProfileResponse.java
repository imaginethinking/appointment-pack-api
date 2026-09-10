package net.imaginethinking.appointmentpack.profile;

import net.imaginethinking.appointmentpack.address.AddressResponse;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents profile information returned by the API.
 */
public record ProfileResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        AddressResponse address
) {

    /**
     * Builds the profile response from the supplied profile.
     */
    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getDateOfBirth(),
                profile.getGender(),
                AddressResponse.from(profile.getAddress())
        );
    }
}