package net.imaginethinking.appointmentpack.patientrecord;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.imaginethinking.appointmentpack.common.BaseEntity;
import net.imaginethinking.appointmentpack.profile.Profile;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "patient_records")
public class PatientRecord extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "profile_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_patient_record_profile")
    )
    private Profile profile;

    // England and Wales regional number
    @Column(name = "nhs_number", length = 10)
    private String nhsNumber;

    // Scotland regional number
    @Column(name = "chi_number", length = 10)
    private String chiNumber;

    // Northern Ireland regional number
    @Column(name = "hc_number", length = 10)
    private String hcNumber;

    @Column(name = "height", precision = 6, scale = 2)
    private BigDecimal height;

    @Enumerated(EnumType.STRING)
    @Column(name = "height_unit")
    private HeightUnit heightUnit;

    @Column(name = "weight", precision = 6, scale = 2)
    private BigDecimal weight;

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_unit")
    private WeightUnit weightUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type")
    private BloodType bloodType;
}
