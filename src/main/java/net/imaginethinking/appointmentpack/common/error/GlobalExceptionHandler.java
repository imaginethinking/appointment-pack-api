package net.imaginethinking.appointmentpack.common.error;

import net.imaginethinking.appointmentpack.document.processing.client.DocumentProcessingException;
import net.imaginethinking.appointmentpack.document.processing.client.DocumentProcessingTimeoutException;
import net.imaginethinking.appointmentpack.document.processing.client.DocumentProcessingUnavailableException;
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

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(
                    error.getField(),
                    error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");

        problem.setTitle("Validation failed");
        problem.setProperty("fieldErrors", fieldErrors);

        return problem;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaximumUploadSize(
            MaxUploadSizeExceededException exception) {
        return createProblem(HttpStatus.PAYLOAD_TOO_LARGE, "File too large", "Document file exceeds the maximum size");
    }

    @ExceptionHandler(DocumentProcessingTimeoutException.class)
    public ProblemDetail handleProcessingTimeout(
            DocumentProcessingTimeoutException exception) {
        return createProblem(HttpStatus.GATEWAY_TIMEOUT, "Processing timed out", "Document processing timed out");
    }

    @ExceptionHandler(DocumentProcessingUnavailableException.class)
    public ProblemDetail handleProcessingUnavailable(
            DocumentProcessingUnavailableException exception) {
        return createProblem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Processing unavailable",
                "Document processing is currently unavailable");
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ProblemDetail handleProcessingFailure(
            DocumentProcessingException exception) {
        return createProblem(
                HttpStatus.BAD_GATEWAY,
                "Processing response invalid",
                "Document processing returned an invalid response");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException exception) {
        return createProblem(
                HttpStatus.CONFLICT,
                "Conflict",
                "The resource was modified by another request. Refresh and try again");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(
            ResponseStatusException exception) {
        String detail = exception.getReason();

        if (detail == null || detail.isBlank()) {
            detail = "The request could not be completed";
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatusCode(), detail);

        problem.setTitle(resolveTitle(exception.getStatusCode().value()));

        return problem;
    }

    private ProblemDetail createProblem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);

        problem.setTitle(title);

        return problem;
    }

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