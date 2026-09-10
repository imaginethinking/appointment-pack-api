package net.imaginethinking.appointmentpack.patientrecord;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.common.TextNormalizer;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import net.imaginethinking.appointmentpack.profile.Profile;
import net.imaginethinking.appointmentpack.profile.ProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Creates and updates patient records, including measurement checks and BMI calculation.
 */
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
    private final PatientRecordAccessService patientRecordAccessService;
    private final AppEventPublisher appEventPublisher;

    /**
     * Loads the current profile, checks that it does not already own a patient record and saves the submitted
     * patient details.
     */
    @Transactional
    public PatientRecordResponse createCurrentPatientRecord(UUID userId, CreatePatientRecordRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));

        if (patientRecordRepository.existsByProfileId(profile.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient record already exists");
        }

        validateMeasurement("Height", request.height(), request.heightUnit());

        validateMeasurement("Weight", request.weight(), request.weightUnit());

        PatientRecord patientRecord = new PatientRecord();

        patientRecord.setProfile(profile);
        patientRecord.setNhsNumber(TextNormalizer.stripToNull(request.nhsNumber()));
        patientRecord.setChiNumber(TextNormalizer.stripToNull(request.chiNumber()));
        patientRecord.setHcNumber(TextNormalizer.stripToNull(request.hcNumber()));
        patientRecord.setHeight(request.height());
        patientRecord.setHeightUnit(request.heightUnit());
        patientRecord.setWeight(request.weight());
        patientRecord.setWeightUnit(request.weightUnit());
        patientRecord.setBloodType(request.bloodType());

        PatientRecord savedPatientRecord = patientRecordRepository.save(patientRecord);

        appEventPublisher.publish(PatientActivityEvent.create(
                userId,
                savedPatientRecord.getId(),
                PatientResourceType.PATIENT_RECORD,
                savedPatientRecord.getId(),
                PatientActivityAction.CREATED));

        return toResponse(savedPatientRecord);
    }

    /**
     * Checks edit access, validates measurement pairs and applies the submitted patient record changes.
     */
    @Transactional
    public PatientRecordResponse updatePatientRecord(
            UUID authenticatedUserId,
            UUID patientRecordId,
            UpdatePatientRecordRequest request) {
        PatientRecord patientRecord = patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecordId,
                PatientRecordPermission.EDIT);

        validateMeasurement("Height", request.height(), request.heightUnit());
        validateMeasurement("Weight", request.weight(), request.weightUnit());

        patientRecord.setNhsNumber(TextNormalizer.stripToNull(request.nhsNumber()));
        patientRecord.setChiNumber(TextNormalizer.stripToNull(request.chiNumber()));
        patientRecord.setHcNumber(TextNormalizer.stripToNull(request.hcNumber()));
        patientRecord.setHeight(request.height());
        patientRecord.setHeightUnit(request.heightUnit());
        patientRecord.setWeight(request.weight());
        patientRecord.setWeightUnit(request.weightUnit());
        patientRecord.setBloodType(request.bloodType());

        appEventPublisher.publish(PatientActivityEvent.create(
                authenticatedUserId,
                patientRecord.getId(),
                PatientResourceType.PATIENT_RECORD,
                patientRecord.getId(),
                PatientActivityAction.UPDATED));

        return toResponse(patientRecord);
    }

    /**
     * Loads the patient record owned by the current user or returns not found when one has not been created.
     */
    @Transactional(readOnly = true)
    public PatientRecordResponse getCurrentPatientRecord(UUID userId) {
        PatientRecord patientRecord = patientRecordRepository.findByProfileUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient record not found"));

        return toResponse(patientRecord);
    }

    /**
     * Loads the selected patient record after checking that the current user can view it.
     */
    @Transactional(readOnly = true)
    public PatientRecordResponse getPatientRecord(UUID authenticatedUserId, UUID patientRecordId) {
        PatientRecord patientRecord = patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecordId,
                PatientRecordPermission.VIEW);

        return toResponse(patientRecord);
    }

    /**
     * Builds the patient record response and calculates BMI from the stored measurements when possible.
     */
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
                patientRecord.getBloodType());
    }

    /**
     * Converts the stored height and weight to metric units and calculates BMI when both measurements are
     * complete.
     */
    private BigDecimal calculateBmi(PatientRecord patientRecord) {
        if (patientRecord.getHeight() == null
                || patientRecord.getHeightUnit() == null
                || patientRecord.getWeight() == null
                || patientRecord.getWeightUnit() == null) {
            return null;
        }

        BigDecimal heightInMeters = convertHeightToMeters(patientRecord.getHeight(), patientRecord.getHeightUnit());

        BigDecimal weightInKilograms = convertWeightToKilograms(
                patientRecord.getWeight(),
                patientRecord.getWeightUnit());

        BigDecimal heightSquared = heightInMeters.multiply(heightInMeters);

        return weightInKilograms.divide(heightSquared, 2, RoundingMode.HALF_UP);
    }

    /**
     * Converts a height from the selected unit into metres for the BMI calculation.
     */
    private BigDecimal convertHeightToMeters(BigDecimal height, HeightUnit unit) {
        return switch (unit) {
            case METERS -> height;
            case CENTIMETERS -> height.divide(CENTIMETERS_PER_METER, 8, RoundingMode.HALF_UP);
            case FEET -> height.multiply(METERS_PER_FOOT);
            case INCHES -> height.multiply(METERS_PER_INCH);
        };
    }

    /**
     * Converts a weight from the selected unit into kilograms for the BMI calculation.
     */
    private BigDecimal convertWeightToKilograms(BigDecimal weight, WeightUnit unit) {
        return switch (unit) {
            case KILOGRAMS -> weight;
            case GRAMS -> weight.divide(GRAMS_PER_KILOGRAM, 8, RoundingMode.HALF_UP);
            case STONE -> weight.multiply(KILOGRAMS_PER_STONE);
            case POUNDS -> weight.multiply(KILOGRAMS_PER_POUND);
        };
    }

    /**
     * Requires a measurement value and its unit to be supplied together.
     */
    private void validateMeasurement(String measurementName, BigDecimal value, Enum<?> unit) {
        if ((value == null) != (unit == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    measurementName + " value and unit must be provided together");
        }
    }
}