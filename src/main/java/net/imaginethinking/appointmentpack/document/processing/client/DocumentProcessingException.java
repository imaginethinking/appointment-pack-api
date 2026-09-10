package net.imaginethinking.appointmentpack.document.processing.client;

/**
 * Represents a failure while working with document processing exception.
 */
public class DocumentProcessingException extends RuntimeException {

    /**
     * Creates a document processing error with the supplied message and optional cause.
     */
    public DocumentProcessingException(String message) {
        super(message);
    }

    /**
     * Creates a document processing error with the supplied message and optional cause.
     */
    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}