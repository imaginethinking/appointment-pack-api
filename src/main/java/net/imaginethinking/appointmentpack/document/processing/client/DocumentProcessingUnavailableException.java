package net.imaginethinking.appointmentpack.document.processing.client;

public class DocumentProcessingUnavailableException extends RuntimeException {

    public DocumentProcessingUnavailableException(Throwable cause) {
        super("Document processing service is unavailable", cause);
    }
}