package net.imaginethinking.appointmentpack.bloodtest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Carries one entered blood result row within a blood test request.
 */
public record BloodTestResultRequest(

        @NotBlank(message = "Analyte name is required") @Size(
                max = 200,
                message = "Analyte name must not exceed 200 characters"
        ) String analyteName,

        @NotBlank(message = "Result value is required") @Size(
                max = 100,
                message = "Result value must not exceed 100 characters"
        ) String resultValue,

        @Size(
                max = 100,
                message = "Unit must not exceed 100 characters"
        ) String unit,

        @Size(
                max = 150,
                message = "Reference range must not exceed 150 characters"
        ) String referenceRange,

        BloodTestResultFlag flag) {
}