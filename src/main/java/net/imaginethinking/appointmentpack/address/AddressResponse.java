package net.imaginethinking.appointmentpack.address;

public record AddressResponse(
        String addressLine1,
        String addressLine2,
        String townCity,
        String county,
        String postcode,
        String country
) {

    public static AddressResponse from(Address address) {
        if (address == null) {
            return null;
        }

        return new AddressResponse(
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getTownCity(),
                address.getCounty(),
                address.getPostcode(),
                address.getCountry()
        );
    }
}