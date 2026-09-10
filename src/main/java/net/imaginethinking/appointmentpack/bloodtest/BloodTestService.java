package net.imaginethinking.appointmentpack.bloodtest;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.common.TextNormalizer;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

// [AI-ASSISTED: ChatGPT, 2026-08-13]
// AI was used to help generate the regular expression for the simple numeric value pattern and
// to help generate the normalisation logic for analyte names.
/**
 * Creates and updates blood tests, including result row validation and safe numeric value parsing.
 */
@Service
@RequiredArgsConstructor
public class BloodTestService {

    private static final Pattern SIMPLE_NUMERIC_VALUE_PATTERN = Pattern.compile("^[+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)$");

    private final BloodTestRepository bloodTestRepository;
    private final PatientRecordAccessService patientRecordAccessService;
    private final AppEventPublisher appEventPublisher;

    /**
     * Checks edit access before saving a new blood test using the submitted values.
     */
    @Transactional
    public BloodTestResponse createBloodTest(
            UUID authenticatedUserId,
            UUID patientRecordId,
            CreateBloodTestRequest request) {
        PatientRecord patientRecord = patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecordId,
                BloodTestPermission.EDIT);

        BloodTest bloodTest = new BloodTest();
        bloodTest.setPatientRecord(patientRecord);

        applyValues(
                bloodTest,
                request.title(),
                request.testDate(),
                request.provider(),
                request.notes(),
                request.results());

        BloodTest savedBloodTest = bloodTestRepository.save(bloodTest);

        publishActivity(
                authenticatedUserId,
                savedBloodTest,
                PatientActivityAction.CREATED);

        return BloodTestResponse.from(savedBloodTest);
    }

    /**
     * Checks view access before returning the active blood tests with the newest test first.
     */
    @Transactional(readOnly = true)
    public List<BloodTestResponse> getBloodTests(UUID authenticatedUserId, UUID patientRecordId) {
        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecordId,
                BloodTestPermission.VIEW);

        return bloodTestRepository
                .findAllByPatientRecord_IdAndArchivedAtIsNullOrderByTestDateDescCreatedAtDesc(patientRecordId)
                .stream()
                .map(BloodTestResponse::from)
                .toList();
    }

    /**
     * Loads the requested blood test and checks that the current user can view its patient record.
     */
    @Transactional(readOnly = true)
    public BloodTestResponse getBloodTest(UUID authenticatedUserId, UUID bloodTestId) {
        BloodTest bloodTest = findAvailableBloodTest(bloodTestId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                bloodTest.getPatientRecord(),
                BloodTestPermission.VIEW);

        return BloodTestResponse.from(bloodTest);
    }

    /**
     * Loads the current blood test, checks edit access and applies the submitted changes.
     */
    @Transactional
    public BloodTestResponse updateBloodTest(
            UUID authenticatedUserId,
            UUID bloodTestId,
            UpdateBloodTestRequest request) {
        BloodTest bloodTest = findAvailableBloodTest(bloodTestId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                bloodTest.getPatientRecord(),
                BloodTestPermission.EDIT);

        applyValues(
                bloodTest,
                request.title(),
                request.testDate(),
                request.provider(),
                request.notes(),
                request.results());

        publishActivity(
                authenticatedUserId,
                bloodTest,
                PatientActivityAction.UPDATED);

        return BloodTestResponse.from(bloodTest);
    }

    /**
     * Checks access and archives the blood test only when it is still active.
     */
    @Transactional
    public BloodTestResponse archiveBloodTest(UUID authenticatedUserId, UUID bloodTestId) {
        BloodTest bloodTest = findBloodTest(bloodTestId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                bloodTest.getPatientRecord(),
                BloodTestPermission.EDIT);

        if (!bloodTest.isArchived()) {
            bloodTest.archive();

            publishActivity(
                    authenticatedUserId,
                    bloodTest,
                    PatientActivityAction.ARCHIVED);
        }

        return BloodTestResponse.from(bloodTest);
    }

    /**
     * Loads the blood test or returns not found when it does not exist.
     */
    private BloodTest findBloodTest(UUID bloodTestId) {
        return bloodTestRepository.findById(bloodTestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood test not found"));
    }

    /**
     * Loads the blood test and treats an archived record as not found.
     */
    private BloodTest findAvailableBloodTest(UUID bloodTestId) {
        BloodTest bloodTest = findBloodTest(bloodTestId);

        if (bloodTest.isArchived()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood test not found");
        }

        return bloodTest;
    }

    /**
     * Publishes the activity event for the completed change.
     */
    private void publishActivity(
            UUID authenticatedUserId,
            BloodTest bloodTest,
            PatientActivityAction action) {
        appEventPublisher.publish(PatientActivityEvent.create(
                authenticatedUserId,
                bloodTest.getPatientRecord().getId(),
                PatientResourceType.BLOOD_TEST,
                bloodTest.getId(),
                action));
    }

    /**
     * Copies the submitted blood test details and replaces its result rows with the current values.
     */
    private void applyValues(
            BloodTest bloodTest,
            String title,
            LocalDate testDate,
            String provider,
            String notes,
            List<BloodTestResultRequest> results) {
        bloodTest.setTitle(TextNormalizer.stripToNull(title));
        bloodTest.setTestDate(testDate);
        bloodTest.setProvider(TextNormalizer.stripToNull(provider));
        bloodTest.setNotes(TextNormalizer.stripToNull(notes));

        replaceResults(bloodTest, results);
    }

    /**
     * Rebuilds the blood result rows in display order and rejects duplicate analytes after normalisation.
     */
    private void replaceResults(BloodTest bloodTest, List<BloodTestResultRequest> inputs) {
        Set<String> analyteKeys = new HashSet<>();

        bloodTest.getResults().clear();

        for (int index = 0; index < inputs.size(); index++) {
            BloodTestResultRequest input = inputs.get(index);

            String analyteName = TextNormalizer.strip(input.analyteName());
            String analyteKey = createAnalyteKey(analyteName);

            // Compare normalised analyte names so small formatting differences cannot create duplicate result rows.
            if (!analyteKeys.add(analyteKey)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A blood test cannot contain duplicate analytes");
            }

            String resultValue = TextNormalizer.strip(input.resultValue());

            BloodTestResult result = new BloodTestResult();

            result.setBloodTest(bloodTest);
            result.setAnalyteName(analyteName);
            result.setAnalyteKey(analyteKey);
            result.setResultValue(resultValue);
            result.setNumericValue(deriveNumericValue(resultValue));
            result.setUnit(TextNormalizer.stripToNull(input.unit()));
            result.setReferenceRange(TextNormalizer.stripToNull(input.referenceRange()));
            result.setFlag(input.flag());
            result.setDisplayOrder(index);

            bloodTest.getResults().add(result);
        }
    }

    /**
     * Normalises an analyte name so duplicate result rows can be detected consistently.
     */
    private String createAnalyteKey(String analyteName) {
        String key = Normalizer.normalize(
                        analyteName.toLowerCase(Locale.ROOT),
                        Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{L}\\p{N}]+", "-")
                .replaceAll("^-+|-+$", "");

        if (key.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Analyte name must contain letters or numbers");
        }

        return key;
    }

    /**
     * Returns a numeric value only when the entered result can be read safely as a simple decimal number.
     */
    private BigDecimal deriveNumericValue(String resultValue) {
        if (!SIMPLE_NUMERIC_VALUE_PATTERN.matcher(resultValue).matches()) {
            return null;
        }

        try {
            BigDecimal numericValue = new BigDecimal(resultValue);

            int scale = Math.max(numericValue.scale(), 0);
            int integerDigits = numericValue.precision() - numericValue.scale();

            if (scale > 10 || integerDigits > 20) {
                return null;
            }

            return numericValue;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}