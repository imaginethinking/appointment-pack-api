package net.imaginethinking.appointmentpack.document.processing.client;

/**
 * Represents a failure while working with document processing timeout exception.
 */
public class DocumentProcessingTimeoutException extends RuntimeException {

    /**
     * Wraps a timeout raised while waiting for the document processing service.
     */
    public DocumentProcessingTimeoutException(Throwable cause) {
        super("Document processing service timed out", cause);
    }
}