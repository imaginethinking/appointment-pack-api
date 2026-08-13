package net.imaginethinking.appointmentpack.document.processing.client;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(
        value = HttpStatus.BAD_GATEWAY,
        reason = "Document processing service request failed"
)
public class DocumentProcessingException extends RuntimeException {

    public DocumentProcessingException(String message) {
        super(message);
    }

    public DocumentProcessingException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}