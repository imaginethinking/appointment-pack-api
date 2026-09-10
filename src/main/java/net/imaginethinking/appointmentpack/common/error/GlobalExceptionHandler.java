package net.imaginethinking.appointmentpack.common.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import net.imaginethinking.appointmentpack.document.processing.client.DocumentProcessingException;
import net.imaginethinking.appointmentpack.document.processing.client.DocumentProcessingTimeoutException;
import net.imaginethinking.appointmentpack.document.processing.client.DocumentProcessingUnavailableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns validation, processing and application errors into consistent HTTP problem responses.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    /**
     * Collects field validation errors and returns them in a bad request problem response.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(
                    error.getField(),
                    error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage());
        }

        return validationProblem(fieldErrors);
    }

    /**
     * Collects constraint violations and returns them in a bad request problem response.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(
            ConstraintViolationException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            fieldErrors.putIfAbsent(
                    resolveConstraintField(violation),
                    violation.getMessage() == null ? "Invalid value" : violation.getMessage());
        }

        return validationProblem(fieldErrors);
    }

    /**
     * Returns a payload too large problem when multipart handling rejects an oversized upload.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaximumUploadSize(
            MaxUploadSizeExceededException exception) {
        return createProblem(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "File too large",
                "Document file exceeds the maximum size");
    }

    /**
     * Returns a gateway timeout problem when document processing exceeds its response timeout.
     */
    @ExceptionHandler(DocumentProcessingTimeoutException.class)
    public ProblemDetail handleProcessingTimeout(
            DocumentProcessingTimeoutException exception) {
        return createProblem(
                HttpStatus.GATEWAY_TIMEOUT,
                "Processing timed out",
                "Document processing timed out");
    }

    /**
     * Returns a service unavailable problem when the document processing service cannot be reached.
     */
    @ExceptionHandler(DocumentProcessingUnavailableException.class)
    public ProblemDetail handleProcessingUnavailable(
            DocumentProcessingUnavailableException exception) {
        return createProblem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Processing unavailable",
                "Document processing is currently unavailable");
    }

    /**
     * Returns a bad gateway problem when document processing fails after the request reaches the service.
     */
    @ExceptionHandler(DocumentProcessingException.class)
    public ProblemDetail handleProcessingFailure(
            DocumentProcessingException exception) {
        return createProblem(
                HttpStatus.BAD_GATEWAY,
                "Processing response invalid",
                "Document processing returned an invalid response");
    }

    /**
     * Returns a conflict problem when another update has changed the same record first.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException exception) {
        return createProblem(
                HttpStatus.CONFLICT,
                "Conflict",
                "The resource was modified by another request. Refresh and try again");
    }

    /**
     * Keeps the status and reason from a ResponseStatusException in the problem response.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(
            ResponseStatusException exception) {
        String detail = exception.getReason();

        if (detail == null || detail.isBlank()) {
            detail = "The request could not be completed";
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                exception.getStatusCode(),
                detail);

        problem.setTitle(resolveTitle(exception.getStatusCode().value()));

        return problem;
    }

    /**
     * Builds the validation problem response and includes the collected field errors.
     */
    private ProblemDetail validationProblem(
            Map<String, String> fieldErrors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed");

        problem.setTitle("Validation failed");
        problem.setProperty("fieldErrors", fieldErrors);

        return problem;
    }

    /**
     * Takes the final property name from a constraint violation path for use in the field error map.
     */
    private String resolveConstraintField(
            ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();

        int separatorIndex = path.lastIndexOf('.');

        if (separatorIndex >= 0 && separatorIndex < path.length() - 1) {
            return path.substring(separatorIndex + 1);
        }

        return path;
    }

    /**
     * Creates a problem response with the supplied status title and detail.
     */
    private ProblemDetail createProblem(
            HttpStatus status,
            String title,
            String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                detail);

        problem.setTitle(title);

        return problem;
    }

    /**
     * Uses the standard HTTP reason phrase as the problem title when one is available.
     */
    private String resolveTitle(int status) {
        return switch (status) {
            case 400 -> "Bad request";
            case 401 -> "Unauthenticated";
            case 403 -> "Forbidden";
            case 404 -> "Not found";
            case 409 -> "Conflict";
            case 413 -> "Payload too large";
            case 415 -> "Unsupported media type";
            case 422 -> "Unprocessable content";
            case 502 -> "Bad gateway";
            case 503 -> "Service unavailable";
            case 504 -> "Gateway timeout";
            default -> "Request failed";
        };
    }
}