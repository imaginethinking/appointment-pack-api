package net.imaginethinking.appointmentpack.profile;

import net.imaginethinking.appointmentpack.address.Address;

import java.time.LocalDate;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        Address address
) {

    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getAddress()
        );
    }
}
