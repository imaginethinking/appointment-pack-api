package net.imaginethinking.appointmentpack.document.processing.client;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE, reason = "Document processing service is unavailable")
public class DocumentProcessingUnavailableException extends RuntimeException {

    public DocumentProcessingUnavailableException(Throwable cause) {
        super("Document processing service is unavailable", cause);
    }
}
