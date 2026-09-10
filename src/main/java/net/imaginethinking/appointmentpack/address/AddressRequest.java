package net.imaginethinking.appointmentpack.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Carries the address fields submitted for a profile or healthcare contact.
 */
public record AddressRequest(

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 150, message = "Address line 1 must not exceed 150 characters")
        String addressLine1,

        @Size(max = 150, message = "Address line 2 must not exceed 150 characters")
        String addressLine2,

        @NotBlank(message = "Town or city is required")
        @Size(max = 100, message = "Town or city must not exceed 100 characters")
        String townCity,

        @Size(max = 100, message = "County must not exceed 100 characters")
        String county,

        @NotBlank(message = "Postcode is required")
        @Size(max = 20, message = "Postcode must not exceed 20 characters")
        String postcode,

        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country
) {
}