package net.imaginethinking.appointmentpack.document.processing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(
        value = HttpStatus.GATEWAY_TIMEOUT,
        reason = "Document processing service timed out"
)
public class DocumentProcessingTimeoutException extends RuntimeException {

    public DocumentProcessingTimeoutException(Throwable cause) {
        super("Document processing service timed out", cause);
    }
}