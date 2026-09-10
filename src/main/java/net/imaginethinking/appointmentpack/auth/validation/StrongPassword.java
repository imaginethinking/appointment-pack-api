package net.imaginethinking.appointmentpack.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// [AI-ASSISTED: ChatGPT, 2026-08-14]
// AI was used to help generate the regular expression for the strong password constraint.
/**
 * Marks a password field that must meet the application password rules.
 */
@Documented
@Constraint(validatedBy = {})
@Target({
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.PARAMETER,
        ElementType.ANNOTATION_TYPE,
        ElementType.TYPE_USE,
        ElementType.RECORD_COMPONENT
})
@Retention(RetentionPolicy.RUNTIME)
@NotBlank
@Size(min = 8, max = 128)
@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])[^\\r\\n]+$")
@ReportAsSingleViolation
public @interface StrongPassword {

    /**
     * Returns the validation message used when a password does not meet the required rules.
     */
    String message() default
            "Password must be 8 to 128 characters and include an uppercase letter, lowercase letter, number, and symbol";

    /**
     * Returns the validation groups that can be assigned to the password constraint.
     */
    Class<?>[] groups() default {};

    /**
     * Returns the validation payload types that can be assigned to the password constraint.
     */
    Class<? extends Payload>[] payload() default {};
}