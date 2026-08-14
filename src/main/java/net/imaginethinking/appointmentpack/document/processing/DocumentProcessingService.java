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

    public DocumentProcessingResultResponse getProcessing(UUID authenticatedUserId, UUID documentId) {
        return documentProcessingStateService.getProcessing(authenticatedUserId, documentId);
    }

    public DocumentProcessingResultResponse extract(UUID authenticatedUserId, UUID documentId) {
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

    public DocumentProcessingResultResponse summarise(
            UUID authenticatedUserId,
            UUID documentId,
            String approvedDeidentifiedText) {
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

    public AppointmentResponse confirmAppointment(
            UUID authenticatedUserId,
            UUID documentId,
            AppointmentConfirmationRequest request) {
        return appointmentDocumentReviewService.confirmAppointment(authenticatedUserId, documentId, request);
    }

    public DocumentProcessingResultResponse rejectAppointment(UUID authenticatedUserId, UUID documentId) {
        return appointmentDocumentReviewService.rejectAppointment(authenticatedUserId, documentId);
    }

    public DocumentProcessingResultResponse acceptSummary(
            UUID authenticatedUserId,
            UUID documentId,
            DocumentSummaryAcceptanceRequest request) {
        return consultationDocumentReviewService.acceptSummary(authenticatedUserId, documentId, request);
    }

    public DocumentProcessingResultResponse rejectSummary(UUID authenticatedUserId, UUID documentId) {
        return consultationDocumentReviewService.rejectSummary(authenticatedUserId, documentId);
    }

    private void recordExtractionFailure(UUID documentId, RuntimeException originalException) {
        try {
            documentProcessingStateService.failExtraction(documentId, EXTRACTION_FAILURE_MESSAGE);
        } catch (RuntimeException persistenceException) {
            originalException.addSuppressed(persistenceException);
        }
    }

    private void recordSummarisationFailure(UUID documentId, RuntimeException originalException) {
        try {
            documentProcessingStateService.failSummarisation(documentId, SUMMARISATION_FAILURE_MESSAGE);
        } catch (RuntimeException persistenceException) {
            originalException.addSuppressed(persistenceException);
        }
    }

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

    private long elapsedMilliseconds(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }
}