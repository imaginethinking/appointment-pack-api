package net.imaginethinking.appointmentpack.patientrecord;

import java.math.BigDecimal;
import java.util.UUID;

public record PatientRecordResponse(
        UUID id,
        UUID profileId,
        String nhsNumber,
        String chiNumber,
        String hcNumber,
        BigDecimal height,
        HeightUnit heightUnit,
        BigDecimal weight,
        WeightUnit weightUnit,
        BigDecimal bmi,
        BloodType bloodType
) {

}
