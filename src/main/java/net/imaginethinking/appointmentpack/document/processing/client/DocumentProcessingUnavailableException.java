package net.imaginethinking.appointmentpack.document.processing.client;

/**
 * Represents a failure while working with document processing unavailable exception.
 */
public class DocumentProcessingUnavailableException extends RuntimeException {

    /**
     * Wraps a connection failure when the document processing service cannot be reached.
     */
    public DocumentProcessingUnavailableException(Throwable cause) {
        super("Document processing service is unavailable", cause);
    }
}