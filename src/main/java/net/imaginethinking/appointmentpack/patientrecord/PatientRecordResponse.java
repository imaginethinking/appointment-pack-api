package net.imaginethinking.appointmentpack.patientrecord;

import net.imaginethinking.appointmentpack.patientrecord.bloodtype.BloodType;
import net.imaginethinking.appointmentpack.patientrecord.measurement.HeightUnit;
import net.imaginethinking.appointmentpack.patientrecord.measurement.WeightUnit;

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
