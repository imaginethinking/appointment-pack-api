package net.imaginethinking.appointmentpack.document.processing;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.appointment.AppointmentConfirmationRequest;
import net.imaginethinking.appointmentpack.appointment.AppointmentResponse;
import net.imaginethinking.appointmentpack.document.processing.api.DocumentProcessingResultResponse;
import net.imaginethinking.appointmentpack.document.processing.api.DocumentSummaryAcceptanceRequest;
import net.imaginethinking.appointmentpack.document.processing.client.*;
import net.imaginethinking.appointmentpack.document.processing.context.DocumentExtractionContext;
import net.imaginethinking.appointmentpack.document.processing.context.DocumentSummarisationContext;
import net.imaginethinking.appointmentpack.document.processing.review.AppointmentDocumentReviewService;
import net.imaginethinking.appointmentpack.document.processing.review.ConsultationDocumentReviewService;
import net.imaginethinking.appointmentpack.document.storage.DocumentStorageService;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingEvent;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingFailureReason;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingOperation;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Runs document extraction and summarisation, then records the result or failure through the document workflow.
 */
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private static final String EXTRACTION_FAILURE_MESSAGE = "Document text could not be extracted.";
    private static final String SUMMARISATION_FAILURE_MESSAGE = "External summary generation is currently unavailable.";

    private final DocumentProcessingStateService documentProcessingStateService;
    private final AppointmentDocumentReviewService appointmentDocumentReviewService;
    private final ConsultationDocumentReviewService consultationDocumentReviewService;
    private final DocumentStorageService documentStorageService;
    private final DocumentProcessingClient documentProcessingClient;
    private final AppEventPublisher appEventPublisher;

    /**
     * Loads the current processing result after applying document access checks.
     */
    public DocumentProcessingResultResponse getProcessing(UUID authenticatedUserId, UUID documentId) {
        return documentProcessingStateService.getProcessing(authenticatedUserId, documentId);
    }

    /**
     * Moves the document into extraction, processes the stored file, then saves the result or records a failed
     * extraction.
     *
     * @param authenticatedUserId user starting the extraction
     * @param documentId document to process
     * @return the processing result after extraction completes
     */
    public DocumentProcessingResultResponse extract(UUID authenticatedUserId, UUID documentId) {
        // Save the extracting state before the slower file and processing work begins.
        DocumentExtractionContext context = documentProcessingStateService.beginExtraction(
                authenticatedUserId,
                documentId);

        long startedAtNanos = System.nanoTime();

        try {
            Resource resource = documentStorageService.load(context.storagePath());

            DocumentExtractionResponse response = documentProcessingClient.extract(context, resource);

            DocumentProcessingResultResponse result = documentProcessingStateService.completeExtraction(
                    context.documentId(),
                    response);

            appEventPublisher.publish(DocumentProcessingEvent.succeeded(
                    authenticatedUserId,
                    context.documentId(),
                    context.documentType(),
                    DocumentProcessingOperation.EXTRACTION,
                    elapsedMilliseconds(startedAtNanos),
                    response.processorVersion(),
                    null,
                    null));

            return result;
        } catch (RuntimeException exception) {
            recordExtractionFailure(context.documentId(), exception);

            appEventPublisher.publish(DocumentProcessingEvent.failed(
                    authenticatedUserId,
                    context.documentId(),
                    context.documentType(),
                    DocumentProcessingOperation.EXTRACTION,
                    elapsedMilliseconds(startedAtNanos),
                    classifyFailure(exception)));

            throw exception;
        }
    }

    /**
     * Saves the approved deidentified text, requests a summary, then stores the result or records a failed
     * summarisation.
     *
     * @param authenticatedUserId user starting the summarisation
     * @param documentId consultation document being summarised
     * @param approvedDeidentifiedText exact reviewed text approved for summarisation
     * @return the processing result after summarisation completes
     */
    public DocumentProcessingResultResponse summarise(
            UUID authenticatedUserId,
            UUID documentId,
            String approvedDeidentifiedText) {
        // Save the approved text and summarising state before making the external processing request.
        DocumentSummarisationContext context = documentProcessingStateService.beginSummarisation(
                authenticatedUserId,
                documentId,
                approvedDeidentifiedText);

        long startedAtNanos = System.nanoTime();

        try {
            DocumentSummaryResponse response = documentProcessingClient.summarise(context);

            DocumentProcessingResultResponse result = documentProcessingStateService.completeSummarisation(
                    context.documentId(),
                    response);

            appEventPublisher.publish(DocumentProcessingEvent.succeeded(
                    authenticatedUserId,
                    context.documentId(),
                    context.documentType(),
                    DocumentProcessingOperation.AI_SUMMARISATION,
                    elapsedMilliseconds(startedAtNanos),
                    response.processorVersion(),
                    response.modelName(),
                    response.promptVersion()));

            return result;
        } catch (RuntimeException exception) {
            recordSummarisationFailure(context.documentId(), exception);

            appEventPublisher.publish(DocumentProcessingEvent.failed(
                    authenticatedUserId,
                    context.documentId(),
                    context.documentType(),
                    DocumentProcessingOperation.AI_SUMMARISATION,
                    elapsedMilliseconds(startedAtNanos),
                    classifyFailure(exception)));

            throw exception;
        }
    }

    /**
     * Passes the reviewed appointment details into the appointment document review flow.
     */
    public AppointmentResponse confirmAppointment(
            UUID authenticatedUserId,
            UUID documentId,
            AppointmentConfirmationRequest request) {
        return appointmentDocumentReviewService.confirmAppointment(authenticatedUserId, documentId, request);
    }

    /**
     * Rejects the appointment suggestions through the appointment document review flow.
     */
    public DocumentProcessingResultResponse rejectAppointment(UUID authenticatedUserId, UUID documentId) {
        return appointmentDocumentReviewService.rejectAppointment(authenticatedUserId, documentId);
    }

    /**
     * Passes the reviewed consultation summary into the final Medical History acceptance flow.
     */
    public DocumentProcessingResultResponse acceptSummary(
            UUID authenticatedUserId,
            UUID documentId,
            DocumentSummaryAcceptanceRequest request) {
        return consultationDocumentReviewService.acceptSummary(authenticatedUserId, documentId, request);
    }

    /**
     * Rejects the generated consultation summary through the consultation review flow.
     */
    public DocumentProcessingResultResponse rejectSummary(UUID authenticatedUserId, UUID documentId) {
        return consultationDocumentReviewService.rejectSummary(authenticatedUserId, documentId);
    }

    /**
     * Records the failed extraction while keeping any persistence error attached to the original processing
     * failure.
     */
    private void recordExtractionFailure(UUID documentId, RuntimeException originalException) {
        try {
            documentProcessingStateService.failExtraction(documentId, EXTRACTION_FAILURE_MESSAGE);
        } catch (RuntimeException persistenceException) {
            originalException.addSuppressed(persistenceException);
        }
    }

    /**
     * Records the failed summarisation while keeping any persistence error attached to the original processing
     * failure.
     */
    private void recordSummarisationFailure(UUID documentId, RuntimeException originalException) {
        try {
            documentProcessingStateService.failSummarisation(documentId, SUMMARISATION_FAILURE_MESSAGE);
        } catch (RuntimeException persistenceException) {
            originalException.addSuppressed(persistenceException);
        }
    }

    /**
     * Groups processing exceptions into the failure reason recorded for operational analytics.
     */
    private DocumentProcessingFailureReason classifyFailure(RuntimeException exception) {
        if (exception instanceof DocumentProcessingTimeoutException) {
            return DocumentProcessingFailureReason.TIMEOUT;
        }

        if (exception instanceof DocumentProcessingUnavailableException) {
            return DocumentProcessingFailureReason.SERVICE_UNAVAILABLE;
        }

        if (exception instanceof DocumentProcessingException) {
            return DocumentProcessingFailureReason.PROCESSING_ERROR;
        }

        if (exception instanceof ResponseStatusException responseStatusException) {
            int status = responseStatusException.getStatusCode().value();

            if (status == 413 || status == 415 || status == 422) {
                return DocumentProcessingFailureReason.PROCESSING_ERROR;
            }
        }

        return DocumentProcessingFailureReason.UNKNOWN;
    }

    /**
     * Returns the elapsed processing time in milliseconds from the recorded start time.
     */
    private long elapsedMilliseconds(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }
}