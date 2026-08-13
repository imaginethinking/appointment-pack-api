package net.imaginethinking.appointmentpack.bloodtest;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.patientcareraccess.PatientAccessControlService;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BloodTestService {

    private static final Pattern SIMPLE_NUMERIC_VALUE_PATTERN = Pattern.compile("^[+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)$");

    private final BloodTestRepository bloodTestRepository;
    private final PatientRecordRepository patientRecordRepository;
    private final PatientAccessControlService patientAccessControlService;

    @Transactional
    public BloodTestResponse createBloodTest(UUID authenticatedUserId, UUID patientRecordId, BloodTestRequest request) {
        PatientRecord patientRecord = findPatientRecord(patientRecordId);

        patientAccessControlService.requirePermission(authenticatedUserId, patientRecord, BloodTestPermission.EDIT);

        BloodTest bloodTest = new BloodTest();
        bloodTest.setPatientRecord(patientRecord);

        applyValues(bloodTest, request);

        BloodTest savedBloodTest = bloodTestRepository.save(bloodTest);

        return BloodTestResponse.from(savedBloodTest);
    }

    @Transactional(readOnly = true)
    public List<BloodTestResponse> getBloodTests(UUID authenticatedUserId, UUID patientRecordId) {
        PatientRecord patientRecord = findPatientRecord(patientRecordId);

        patientAccessControlService.requirePermission(authenticatedUserId, patientRecord, BloodTestPermission.VIEW);

        return bloodTestRepository.findAllByPatientRecord_IdAndArchivedAtIsNullOrderByTestDateDescCreatedAtDesc(
                patientRecordId).stream().map(BloodTestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BloodTestResponse getBloodTest(UUID authenticatedUserId, UUID bloodTestId) {
        BloodTest bloodTest = findAvailableBloodTest(bloodTestId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                bloodTest.getPatientRecord(),
                BloodTestPermission.VIEW);

        return BloodTestResponse.from(bloodTest);
    }

    @Transactional
    public BloodTestResponse updateBloodTest(UUID authenticatedUserId, UUID bloodTestId, BloodTestRequest request) {
        BloodTest bloodTest = findAvailableBloodTest(bloodTestId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                bloodTest.getPatientRecord(),
                BloodTestPermission.EDIT);

        applyValues(bloodTest, request);

        return BloodTestResponse.from(bloodTest);
    }

    @Transactional
    public BloodTestResponse archiveBloodTest(UUID authenticatedUserId, UUID bloodTestId) {
        BloodTest bloodTest = findBloodTest(bloodTestId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                bloodTest.getPatientRecord(),
                BloodTestPermission.EDIT);

        bloodTest.archive();

        return BloodTestResponse.from(bloodTest);
    }

    private PatientRecord findPatientRecord(UUID patientRecordId) {
        return patientRecordRepository.findById(patientRecordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient record not found"));
    }

    private BloodTest findBloodTest(UUID bloodTestId) {
        return bloodTestRepository.findById(bloodTestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood test not found"));
    }

    private BloodTest findAvailableBloodTest(UUID bloodTestId) {
        BloodTest bloodTest = findBloodTest(bloodTestId);

        if (bloodTest.isArchived()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood test not found");
        }

        return bloodTest;
    }

    private void applyValues(BloodTest bloodTest, BloodTestRequest request) {
        bloodTest.setTitle(normaliseOptionalValue(request.title()));
        bloodTest.setTestDate(request.testDate());
        bloodTest.setProvider(normaliseOptionalValue(request.provider()));
        bloodTest.setNotes(normaliseOptionalValue(request.notes()));

        replaceResults(bloodTest, request.results());
    }

    private void replaceResults(BloodTest bloodTest, List<BloodTestRequest.ResultInput> inputs) {
        Set<String> analyteKeys = new HashSet<>();

        bloodTest.getResults().clear();

        for (int index = 0; index < inputs.size(); index++) {
            BloodTestRequest.ResultInput input = inputs.get(index);

            String analyteName = input.analyteName().strip();
            String analyteKey = createAnalyteKey(analyteName);

            if (!analyteKeys.add(analyteKey)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A blood test cannot contain duplicate analytes");
            }

            String resultValue = input.resultValue().strip();

            BloodTestResult result = new BloodTestResult();

            result.setBloodTest(bloodTest);
            result.setAnalyteName(analyteName);
            result.setAnalyteKey(analyteKey);
            result.setResultValue(resultValue);
            result.setNumericValue(deriveNumericValue(resultValue));
            result.setUnit(normaliseOptionalValue(input.unit()));
            result.setReferenceRange(normaliseOptionalValue(input.referenceRange()));
            result.setFlag(input.flag());
            result.setDisplayOrder(index);

            bloodTest.getResults().add(result);
        }
    }

    private String createAnalyteKey(String analyteName) {
        String key = Normalizer.normalize(analyteName.toLowerCase(Locale.ROOT), Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{L}\\p{N}]+", "-")
                .replaceAll("^-+|-+$", "");

        if (key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Analyte name must contain letters or numbers");
        }

        return key;
    }

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

    private String normaliseOptionalValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.strip();
    }
}