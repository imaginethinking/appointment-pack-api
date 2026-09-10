package net.imaginethinking.appointmentpack.patientrecord;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Carries the values submitted when creating patient record.
 */
public record CreatePatientRecordRequest(
        @Size(max = 10)
        String nhsNumber,

        @Size(max = 10)
        String chiNumber,

        @Size(max = 10)
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