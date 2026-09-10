package net.imaginethinking.appointmentpack.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Carries the changes submitted when updating emergency contact.
 */
public record UpdateEmergencyContactRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @NotBlank
        @Size(max = 150)
        String relationship,

        @NotBlank
        @Size(max = 50)
        String phoneNumber,

        @Size(max = 50)
        String alternativePhoneNumber,

        @Email
        @Size(max = 254)
        String email,

        @Size(max = 2000)
        String notes
) {
}