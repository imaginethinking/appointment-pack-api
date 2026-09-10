package net.imaginethinking.appointmentpack.document.processing;

import net.imaginethinking.appointmentpack.document.DocumentStatus;
import net.imaginethinking.appointmentpack.document.DocumentType;
import net.imaginethinking.appointmentpack.document.processing.api.DocumentProcessingResultResponse;
import net.imaginethinking.appointmentpack.document.processing.client.*;
import net.imaginethinking.appointmentpack.document.processing.context.DocumentExtractionContext;
import net.imaginethinking.appointmentpack.document.processing.context.DocumentSummarisationContext;
import net.imaginethinking.appointmentpack.document.processing.context.RedactionContext;
import net.imaginethinking.appointmentpack.document.processing.review.AppointmentDocumentReviewService;
import net.imaginethinking.appointmentpack.document.processing.review.ConsultationDocumentReviewService;
import net.imaginethinking.appointmentpack.document.storage.DocumentStorageService;
import net.imaginethinking.appointmentpack.event.AppEvent;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingEvent;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingFailureReason;
import net.imaginethinking.appointmentpack.event.processing.DocumentProcessingOperation;
import net.imaginethinking.appointmentpack.event.processing.ProcessingOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks document processing service behaviour across normal and failure cases.
 */
@ExtendWith(MockitoExtension.class)
class DocumentProcessingServiceTest {

    @Mock
    private DocumentProcessingStateService documentProcessingStateService;

    @Mock
    private AppointmentDocumentReviewService appointmentDocumentReviewService;

    @Mock
    private ConsultationDocumentReviewService consultationDocumentReviewService;

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private DocumentProcessingClient documentProcessingClient;

    @Mock
    private AppEventPublisher appEventPublisher;

    private DocumentProcessingService service;

    /**
     * Creates the common fixtures and mocks used by each test.
     */
    @BeforeEach
    void setUp() {
        service = new DocumentProcessingService(
                documentProcessingStateService,
                appointmentDocumentReviewService,
                consultationDocumentReviewService,
                documentStorageService,
                documentProcessingClient,
                appEventPublisher);
    }

    @Test
    void shouldOrchestrateSuccessfulExtractionAndPublishAnalytics() {
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        DocumentExtractionContext context = new DocumentExtractionContext(
                documentId,
                DocumentType.APPOINTMENT_LETTER,
                "letter.pdf",
                "application/pdf",
                "ab/letter.pdf",
                RedactionContext.empty());

        DocumentExtractionResponse extractionResponse = new DocumentExtractionResponse(
                documentId,
                "Extracted text",
                null,
                null,
                null,
                "processor-1");

        DocumentProcessingResultResponse completedResponse = response(
                documentId,
                DocumentType.APPOINTMENT_LETTER,
                DocumentStatus.READY_FOR_APPOINTMENT_REVIEW);

        ByteArrayResource resource = new ByteArrayResource(new byte[]{1, 2, 3});

        when(documentProcessingStateService.beginExtraction(userId, documentId)).thenReturn(context);

        when(documentStorageService.load("ab/letter.pdf")).thenReturn(resource);

        when(documentProcessingClient.extract(context, resource)).thenReturn(extractionResponse);

        when(documentProcessingStateService.completeExtraction(documentId, extractionResponse)).thenReturn(
                completedResponse);

        DocumentProcessingResultResponse result = service.extract(userId, documentId);

        assertSame(completedResponse, result);

        DocumentProcessingEvent event = capturedProcessingEvent();

        assertEquals(DocumentProcessingOperation.EXTRACTION, event.operation());

        assertEquals(ProcessingOutcome.SUCCEEDED, event.outcome());

        assertEquals("processor-1", event.processorVersion());
    }

    @Test
    void shouldRecordTimeoutWhenExtractionFails() {
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        DocumentExtractionContext context = new DocumentExtractionContext(
                documentId,
                DocumentType.CONSULTATION_OUTCOME_LETTER,
                "letter.pdf",
                "application/pdf",
                "ab/letter.pdf",
                new RedactionContext(java.util.List.of("Patient")));

        ByteArrayResource resource = new ByteArrayResource(new byte[]{1});

        DocumentProcessingTimeoutException timeout = new DocumentProcessingTimeoutException(new RuntimeException(
                "timeout"));

        when(documentProcessingStateService.beginExtraction(userId, documentId)).thenReturn(context);

        when(documentStorageService.load("ab/letter.pdf")).thenReturn(resource);

        when(documentProcessingClient.extract(context, resource)).thenThrow(timeout);

        DocumentProcessingTimeoutException thrown = assertThrows(
                DocumentProcessingTimeoutException.class,
                () -> service.extract(userId, documentId));

        assertSame(timeout, thrown);

        verify(documentProcessingStateService).failExtraction(documentId, "Document text could not be extracted.");

        DocumentProcessingEvent event = capturedProcessingEvent();

        assertEquals(ProcessingOutcome.FAILED, event.outcome());

        assertEquals(DocumentProcessingFailureReason.TIMEOUT, event.failureReason());
    }

    @Test
    void shouldOrchestrateSuccessfulSummarisationAndPublishAnalytics() {
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        DocumentSummarisationContext context = new DocumentSummarisationContext(
                documentId,
                DocumentType.CONSULTATION_OUTCOME_LETTER,
                "Approved text");

        DocumentSummaryResponse summaryResponse = new DocumentSummaryResponse(
                documentId,
                "Generated summary",
                "processor-2",
                "model-a",
                "prompt-v1");

        DocumentProcessingResultResponse completedResponse = response(
                documentId,
                DocumentType.CONSULTATION_OUTCOME_LETTER,
                DocumentStatus.READY_FOR_SUMMARY_REVIEW);

        when(documentProcessingStateService.beginSummarisation(
                userId,
                documentId,
                "Approved text")).thenReturn(context);

        when(documentProcessingClient.summarise(context)).thenReturn(summaryResponse);

        when(documentProcessingStateService.completeSummarisation(documentId, summaryResponse)).thenReturn(
                completedResponse);

        DocumentProcessingResultResponse result = service.summarise(userId, documentId, "Approved text");

        assertSame(completedResponse, result);

        DocumentProcessingEvent event = capturedProcessingEvent();

        assertEquals(DocumentProcessingOperation.AI_SUMMARISATION, event.operation());

        assertEquals(ProcessingOutcome.SUCCEEDED, event.outcome());

        assertEquals("model-a", event.modelName());

        assertEquals("prompt-v1", event.promptVersion());
    }

    @Test
    void shouldClassifyExpectedProcessingClientErrorAsProcessingError() {
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        DocumentSummarisationContext context = new DocumentSummarisationContext(
                documentId,
                DocumentType.CONSULTATION_OUTCOME_LETTER,
                "Approved text");

        ResponseStatusException clientError = new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Invalid processing input");

        when(documentProcessingStateService.beginSummarisation(
                userId,
                documentId,
                "Approved text")).thenReturn(context);

        when(documentProcessingClient.summarise(context)).thenThrow(clientError);

        assertThrows(ResponseStatusException.class, () -> service.summarise(userId, documentId, "Approved text"));

        verify(documentProcessingStateService).failSummarisation(
                documentId,
                "External summary generation is currently unavailable.");

        DocumentProcessingEvent event = capturedProcessingEvent();

        assertEquals(DocumentProcessingFailureReason.PROCESSING_ERROR, event.failureReason());
    }

    @Test
    void shouldClassifyInvalidProcessingResponseAsProcessingError() {
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        DocumentExtractionContext context = new DocumentExtractionContext(
                documentId,
                DocumentType.APPOINTMENT_LETTER,
                "letter.pdf",
                "application/pdf",
                "ab/letter.pdf",
                RedactionContext.empty());

        DocumentProcessingException failure = new DocumentProcessingException("invalid response");

        when(documentProcessingStateService.beginExtraction(userId, documentId)).thenReturn(context);

        when(documentStorageService.load("ab/letter.pdf")).thenThrow(failure);

        assertThrows(DocumentProcessingException.class, () -> service.extract(userId, documentId));

        DocumentProcessingEvent event = capturedProcessingEvent();

        assertEquals(DocumentProcessingFailureReason.PROCESSING_ERROR, event.failureReason());
    }

    /**
     * Returns the document processing event captured during the test.
     */
    private DocumentProcessingEvent capturedProcessingEvent() {
        ArgumentCaptor<AppEvent> eventCaptor = ArgumentCaptor.forClass(AppEvent.class);

        verify(appEventPublisher).publish(eventCaptor.capture());

        return (DocumentProcessingEvent) eventCaptor.getValue();
    }

    /**
     * Creates the response returned to the current test.
     */
    private DocumentProcessingResultResponse response(
            UUID documentId,
            DocumentType documentType,
            DocumentStatus status) {
        return new DocumentProcessingResultResponse(
                documentId,
                documentType,
                status,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}