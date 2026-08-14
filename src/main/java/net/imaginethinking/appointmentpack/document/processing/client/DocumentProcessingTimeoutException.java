package net.imaginethinking.appointmentpack.document.processing.client;

public class DocumentProcessingTimeoutException extends RuntimeException {

    public DocumentProcessingTimeoutException(Throwable cause) {
        super("Document processing service timed out", cause);
    }
}