package net.imaginethinking.appointmentpack.address;

import net.imaginethinking.appointmentpack.common.TextNormalizer;

public final class AddressMapper {

    private AddressMapper() {
    }

    public static Address toAddress(AddressRequest request) {
        if (request == null) {
            return null;
        }

        Address address = new Address();

        address.setAddressLine1(TextNormalizer.strip(request.addressLine1()));
        address.setAddressLine2(TextNormalizer.stripToNull(request.addressLine2()));
        address.setTownCity(TextNormalizer.strip(request.townCity()));
        address.setCounty(TextNormalizer.stripToNull(request.county()));
        address.setPostcode(TextNormalizer.strip(request.postcode()));
        address.setCountry(TextNormalizer.strip(request.country()));

        return address;
    }

    public static Address toAddress(PartialAddressRequest request) {
        if (request == null) {
            return null;
        }

        String addressLine1 = TextNormalizer.stripToNull(request.addressLine1());
        String addressLine2 = TextNormalizer.stripToNull(request.addressLine2());
        String townCity = TextNormalizer.stripToNull(request.townCity());
        String county = TextNormalizer.stripToNull(request.county());
        String postcode = TextNormalizer.stripToNull(request.postcode());
        String country = TextNormalizer.stripToNull(request.country());

        if (addressLine1 == null
                && addressLine2 == null
                && townCity == null
                && county == null
                && postcode == null
                && country == null) {
            return null;
        }

        Address address = new Address();

        address.setAddressLine1(addressLine1);
        address.setAddressLine2(addressLine2);
        address.setTownCity(townCity);
        address.setCounty(county);
        address.setPostcode(postcode);
        address.setCountry(country);

        return address;
    }
}