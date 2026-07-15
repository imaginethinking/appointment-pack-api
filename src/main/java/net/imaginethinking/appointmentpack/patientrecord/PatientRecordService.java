package net.imaginethinking.appointmentpack.patientrecord;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.patientrecord.measurement.HeightUnit;
import net.imaginethinking.appointmentpack.patientrecord.measurement.WeightUnit;
import net.imaginethinking.appointmentpack.profile.Profile;
import net.imaginethinking.appointmentpack.profile.ProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientRecordService {
    private static final BigDecimal CENTIMETERS_PER_METER = new BigDecimal("100");
    private static final BigDecimal METERS_PER_FOOT = new BigDecimal("0.3048");
    private static final BigDecimal METERS_PER_INCH = new BigDecimal("0.0254");
    private static final BigDecimal GRAMS_PER_KILOGRAM = new BigDecimal("1000");
    private static final BigDecimal KILOGRAMS_PER_STONE = new BigDecimal("6.35029318");
    private static final BigDecimal KILOGRAMS_PER_POUND = new BigDecimal("0.45359237");

    private final PatientRecordRepository patientRecordRepository;
    private final ProfileRepository profileRepository;

    @Transactional
    public PatientRecordResponse createCurrentPatientRecord(UUID userId, CreatePatientRecordRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Profile not found"
                ));

        if (patientRecordRepository.existsByProfileId(profile.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Patient record already exists"
            );
        }

        validateMeasurement("Height", request.height(), request.heightUnit());

        validateMeasurement("Weight", request.weight(), request.weightUnit());

        PatientRecord patientRecord = new PatientRecord();
        patientRecord.setProfile(profile);
        patientRecord.setNhsNumber(normalise(request.nhsNumber()));
        patientRecord.setChiNumber(normalise(request.chiNumber()));
        patientRecord.setHcNumber(normalise(request.hcNumber()));
        patientRecord.setHeight(request.height());
        patientRecord.setHeightUnit(request.heightUnit());
        patientRecord.setWeight(request.weight());
        patientRecord.setWeightUnit(request.weightUnit());
        patientRecord.setBloodType(request.bloodType());

        PatientRecord savedPatientRecord = patientRecordRepository.save(patientRecord);

        return toResponse(savedPatientRecord);
    }

    @Transactional(readOnly = true)
    public PatientRecordResponse getCurrentPatientRecord(UUID userId) {
        PatientRecord patientRecord = patientRecordRepository
                .findByProfileUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient record not found"
                ));

        return toResponse(patientRecord);
    }

    private PatientRecordResponse toResponse(PatientRecord patientRecord) {
        return new PatientRecordResponse(
                patientRecord.getId(),
                patientRecord.getProfile().getId(),
                patientRecord.getNhsNumber(),
                patientRecord.getChiNumber(),
                patientRecord.getHcNumber(),
                patientRecord.getHeight(),
                patientRecord.getHeightUnit(),
                patientRecord.getWeight(),
                patientRecord.getWeightUnit(),
                calculateBmi(patientRecord),
                patientRecord.getBloodType()
        );
    }

    private BigDecimal calculateBmi(PatientRecord patientRecord) {
        if (patientRecord.getHeight() == null
                || patientRecord.getHeightUnit() == null
                || patientRecord.getWeight() == null
                || patientRecord.getWeightUnit() == null) {
            return null;
        }

        BigDecimal heightInMeters = convertHeightToMeters(
                patientRecord.getHeight(),
                patientRecord.getHeightUnit()
        );

        BigDecimal weightInKilograms = convertWeightToKilograms(
                patientRecord.getWeight(),
                patientRecord.getWeightUnit()
        );

        BigDecimal heightSquared = heightInMeters.multiply(heightInMeters);

        return weightInKilograms.divide(
                heightSquared,
                2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal convertHeightToMeters(
            BigDecimal height,
            HeightUnit unit
    ) {
        return switch (unit) {
            case METERS -> height;
            case CENTIMETERS -> height.divide(
                    CENTIMETERS_PER_METER,
                    8,
                    RoundingMode.HALF_UP
            );
            case FEET -> height.multiply(METERS_PER_FOOT);
            case INCHES -> height.multiply(METERS_PER_INCH);
        };
    }

    private BigDecimal convertWeightToKilograms(
            BigDecimal weight,
            WeightUnit unit
    ) {
        return switch (unit) {
            case KILOGRAMS -> weight;
            case GRAMS -> weight.divide(
                    GRAMS_PER_KILOGRAM,
                    8,
                    RoundingMode.HALF_UP
            );
            case STONE -> weight.multiply(KILOGRAMS_PER_STONE);
            case POUNDS -> weight.multiply(KILOGRAMS_PER_POUND);
        };
    }

    private void validateMeasurement(String measurementName, BigDecimal value, Enum<?> unit) {
        if ((value == null) != (unit == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    measurementName + " value and unit must be provided together"
            );
        }
    }

    private String normalise(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
