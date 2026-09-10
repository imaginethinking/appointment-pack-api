package net.imaginethinking.appointmentpack.bloodtest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Represents blood test information returned by the API.
 */
public record BloodTestResponse(
        UUID id,
        UUID patientRecordId,
        String title,
        LocalDate testDate,
        String provider,
        String notes,
        Instant archivedAt,
        List<ResultResponse> results,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Builds the blood test response from the supplied blood test.
     */
    public static BloodTestResponse from(BloodTest bloodTest) {
        List<ResultResponse> results = bloodTest
                .getResults()
                .stream()
                .sorted(Comparator.comparingInt(
                        BloodTestResult::getDisplayOrder
                ))
                .map(ResultResponse::from)
                .toList();

        return new BloodTestResponse(
                bloodTest.getId(),
                bloodTest.getPatientRecord().getId(),
                bloodTest.getTitle(),
                bloodTest.getTestDate(),
                bloodTest.getProvider(),
                bloodTest.getNotes(),
                bloodTest.getArchivedAt(),
                results,
                bloodTest.getCreatedAt(),
                bloodTest.getUpdatedAt()
        );
    }

    /**
     * Represents result information returned by the API.
     */
    public record ResultResponse(
            String analyteName,
            String analyteKey,
            String resultValue,
            BigDecimal numericValue,
            String unit,
            String referenceRange,
            BloodTestResultFlag flag
    ) {

        /**
         * Builds the result response from the supplied blood test result.
         */
        public static ResultResponse from(BloodTestResult result) {
            return new ResultResponse(
                    result.getAnalyteName(),
                    result.getAnalyteKey(),
                    result.getResultValue(),
                    result.getNumericValue(),
                    result.getUnit(),
                    result.getReferenceRange(),
                    result.getFlag()
            );
        }
    }
}