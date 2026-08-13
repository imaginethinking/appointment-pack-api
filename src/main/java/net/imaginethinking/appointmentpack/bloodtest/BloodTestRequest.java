package net.imaginethinking.appointmentpack.bloodtest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record BloodTestRequest(

        @Size(max = 200)
        String title,

        @NotNull(message = "Blood test date is required")
        LocalDate testDate,

        @Size(max = 200)
        String provider,

        @Size(max = 2000)
        String notes,

        @Valid
        @NotEmpty(message = "At least one blood test result is required")
        @Size(
                max = 100,
                message = "A blood test cannot contain more than 100 results"
        )
        List<ResultInput> results
) {

    public record ResultInput(

            @NotBlank(message = "Analyte name is required")
            @Size(max = 200)
            String analyteName,

            @NotBlank(message = "Result value is required")
            @Size(max = 100)
            String resultValue,

            @Size(max = 100)
            String unit,

            @Size(max = 150)
            String referenceRange,

            BloodTestResultFlag flag
    ) {
    }
}