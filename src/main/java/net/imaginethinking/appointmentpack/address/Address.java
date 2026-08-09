package net.imaginethinking.appointmentpack.address;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class Address {

    @Column(name = "address_line_1", length = 150)
    private String addressLine1;

    @Column(name = "address_line_2", length = 150)
    private String addressLine2;

    @Column(name = "town_city", length = 100)
    private String townCity;

    @Column(name = "county", length = 100)
    private String county;

    @Column(name = "postcode", length = 20)
    private String postcode;

    @Column(name = "country", length = 100)
    private String country;
}