package net.imaginethinking.appointmentpack.contact;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import net.imaginethinking.appointmentpack.address.AddressRequest;

/**
 * Carries the changes submitted when updating healthcare contact.
 */
public record UpdateHealthcareContactRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 150)
        String role,

        @Size(max = 200)
        String organisation,

        @Size(max = 50)
        String phoneNumber,

        @Email
        @Size(max = 254)
        String email,

        @Valid
        AddressRequest address,

        @Size(max = 2000)
        String notes
) {
}