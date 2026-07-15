package net.imaginethinking.appointmentpack.patientrecord;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import net.imaginethinking.appointmentpack.patientrecord.bloodtype.BloodType;
import net.imaginethinking.appointmentpack.patientrecord.measurement.HeightUnit;
import net.imaginethinking.appointmentpack.patientrecord.measurement.WeightUnit;

import java.math.BigDecimal;

public record CreatePatientRecordRequest(
        @Size(max = 20)
        String nhsNumber,

        @Size(max = 20)
        String chiNumber,

        @Size(max = 20)
        String hcNumber,

        @DecimalMin("0.01")
        @Digits(integer = 4, fraction = 2)
        BigDecimal height,

        HeightUnit heightUnit,

        @DecimalMin("0.01")
        @Digits(integer = 4, fraction = 2)
        BigDecimal weight,

        WeightUnit weightUnit,

        BloodType bloodType
) {
}
