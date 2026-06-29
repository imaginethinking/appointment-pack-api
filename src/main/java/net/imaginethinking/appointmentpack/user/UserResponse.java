package net.imaginethinking.appointmentpack.user;

import java.time.LocalDate;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        UserRole role,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getFirstName(),
                user.getLastName(),
                user.getDateOfBirth(),
                user.getGender()
        );
    }
}
