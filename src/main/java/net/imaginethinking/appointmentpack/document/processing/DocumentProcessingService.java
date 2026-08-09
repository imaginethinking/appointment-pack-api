package net.imaginethinking.appointmentpack.document.processing;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.appointment.AppointmentConfirmationRequest;
import net.imaginethinking.appointmentpack.appointment.AppointmentResponse;
import net.imaginethinking.appointmentpack.document.storage.DocumentStorageService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private static final String EXTRACTION_FAILURE_MESSAGE = "Document text could not be extracted.";
    private static final String SUMMARISATION_FAILURE_MESSAGE = "External summary generation is currently unavailable.";

    private final DocumentProcessingStateService documentProcessingStateService;
    private final DocumentStorageService documentStorageService;
    private final DocumentProcessingClient documentProcessingClient;

    public DocumentProcessingResultResponse getProcessing(UUID authenticatedUserId, UUID documentId) {
        return documentProcessingStateService.getProcessing(authenticatedUserId, documentId);
    }

    public DocumentProcessingResultResponse extract(UUID authenticatedUserId, UUID documentId) {
        DocumentExtractionContext context = documentProcessingStateService.beginExtraction(
                authenticatedUserId,
                documentId);

        try {
            Resource resource = documentStorageService.load(context.storagePath());

            DocumentExtractionResponse response = documentProcessingClient.extract(context, resource);

            return documentProcessingStateService.completeExtraction(context.documentId(), response);
        } catch (RuntimeException exception) {
            recordExtractionFailure(context.documentId(), exception);

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

        try {
            DocumentSummaryResponse response = documentProcessingClient.summarise(context);

            return documentProcessingStateService.completeSummarisation(context.documentId(), response);
        } catch (RuntimeException exception) {
            recordSummarisationFailure(context.documentId(), exception);

            throw exception;
        }
    }

    public AppointmentResponse confirmAppointment(
            UUID authenticatedUserId,
            UUID documentId,
            AppointmentConfirmationRequest request) {
        return documentProcessingStateService.confirmAppointment(authenticatedUserId, documentId, request);
    }

    public DocumentProcessingResultResponse rejectAppointment(UUID authenticatedUserId, UUID documentId) {
        return documentProcessingStateService.rejectAppointment(authenticatedUserId, documentId);
    }

    public DocumentProcessingResultResponse acceptSummary(
            UUID authenticatedUserId,
            UUID documentId,
            DocumentSummaryAcceptanceRequest request) {
        return documentProcessingStateService.acceptSummary(authenticatedUserId, documentId, request);
    }

    public DocumentProcessingResultResponse rejectSummary(UUID authenticatedUserId, UUID documentId) {
        return documentProcessingStateService.rejectSummary(authenticatedUserId, documentId);
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
}