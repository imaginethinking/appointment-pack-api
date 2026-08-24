package net.imaginethinking.appointmentpack.document.processing;

import jakarta.persistence.EntityManager;
import net.imaginethinking.appointmentpack.document.*;
import net.imaginethinking.appointmentpack.document.processing.api.AppointmentDetailsResponse;
import net.imaginethinking.appointmentpack.document.processing.api.DocumentProcessingResultResponse;
import net.imaginethinking.appointmentpack.document.processing.client.DocumentExtractionResponse;
import net.imaginethinking.appointmentpack.document.processing.client.DocumentSummaryResponse;
import net.imaginethinking.appointmentpack.document.processing.context.DocumentExtractionContext;
import net.imaginethinking.appointmentpack.document.processing.context.DocumentSummarisationContext;
import net.imaginethinking.appointmentpack.document.processing.context.RedactionContext;
import net.imaginethinking.appointmentpack.document.processing.context.RedactionContextFactory;
import net.imaginethinking.appointmentpack.event.AppEvent;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import net.imaginethinking.appointmentpack.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingStateServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentProcessingResultRepository processingResultRepository;

    @Mock
    private RedactionContextFactory redactionContextFactory;

    @Mock
    private DocumentProcessingRecordService processingRecordService;

    @Mock
    private PatientRecordAccessService patientRecordAccessService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private AppEventPublisher appEventPublisher;

    @Mock
    private DocumentProcessingLifecyclePolicy lifecyclePolicy;

    private DocumentProcessingStateService service;

    @BeforeEach
    void setUp() {
        service = new DocumentProcessingStateService(
                documentRepository,
                processingResultRepository,
                redactionContextFactory,
                processingRecordService,
                patientRecordAccessService,
                new DocumentProcessingResultMapper(),
                entityManager,
                appEventPublisher,
                lifecyclePolicy);
    }

    @Test
    void shouldReturnDocumentStateBeforeProcessingResultExists() {
        UUID userId = UUID.randomUUID();

        Document document = document(DocumentType.APPOINTMENT_LETTER, DocumentStatus.UPLOADED);

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(processingRecordService.findProcessingResult(document.getId())).thenReturn(Optional.empty());

        DocumentProcessingResultResponse response = service.getProcessing(userId, document.getId());

        assertEquals(document.getId(), response.documentId());
        assertEquals(DocumentStatus.UPLOADED, response.status());
        assertNull(response.extractedText());
    }

    @Test
    void shouldBeginAppointmentExtractionWithoutPatientRedactionContext() {
        UUID userId = UUID.randomUUID();

        Document document = document(DocumentType.APPOINTMENT_LETTER, DocumentStatus.UPLOADED);

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(lifecyclePolicy.isStale(any(Document.class), any())).thenReturn(false);

        DocumentExtractionContext context = service.beginExtraction(userId, document.getId());

        assertEquals(DocumentStatus.EXTRACTING, document.getStatus());
        assertTrue(context.redactionContext().knownValues().isEmpty());

        verify(redactionContextFactory, never()).create(any());

        verify(patientRecordAccessService).requireAccess(userId, document.getPatientRecord(), DocumentPermission.EDIT);
    }

    @Test
    void shouldUseRedactionContextForConsultationExtraction() {
        UUID userId = UUID.randomUUID();

        Document document = document(DocumentType.CONSULTATION_OUTCOME_LETTER, DocumentStatus.UPLOADED);

        RedactionContext redactionContext = new RedactionContext(List.of("Example Patient"));

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(lifecyclePolicy.isStale(any(Document.class), any())).thenReturn(false);

        when(redactionContextFactory.create(document)).thenReturn(redactionContext);

        DocumentExtractionContext context = service.beginExtraction(userId, document.getId());

        assertEquals(List.of("Example Patient"), context.redactionContext().knownValues());

        assertEquals(DocumentStatus.EXTRACTING, document.getStatus());
    }

    @Test
    void shouldCompleteAppointmentExtractionReadyForReview() {
        Document document = document(DocumentType.APPOINTMENT_LETTER, DocumentStatus.EXTRACTING);

        AppointmentDetailsResponse details = new AppointmentDetailsResponse(
                LocalDate.of(2026, 9, 10),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30),
                "Example Service",
                "Follow-up",
                "Example Clinical Team",
                "Example Clinic",
                null);

        DocumentExtractionResponse extractionResponse = new DocumentExtractionResponse(
                document.getId(),
                "Extracted appointment text",
                null,
                details,
                null,
                "processor-1");

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(processingResultRepository.findByDocumentId(document.getId())).thenReturn(Optional.empty());

        when(processingResultRepository.save(any(DocumentProcessingResult.class))).thenAnswer(invocation -> invocation.getArgument(
                0));

        DocumentProcessingResultResponse response = service.completeExtraction(document.getId(), extractionResponse);

        assertEquals(DocumentStatus.READY_FOR_APPOINTMENT_REVIEW, document.getStatus());

        assertEquals(DocumentStatus.READY_FOR_APPOINTMENT_REVIEW, response.status());

        assertEquals("Example Service", response.appointmentDetails().service());

        assertNull(response.machineDeidentifiedText());
    }

    @Test
    void shouldCompleteConsultationExtractionReadyForDeidentificationReview() {
        Document document = document(DocumentType.CONSULTATION_OUTCOME_LETTER, DocumentStatus.EXTRACTING);

        DocumentExtractionResponse extractionResponse = new DocumentExtractionResponse(
                document.getId(),
                "Original consultation text",
                "Machine de-identified text",
                null,
                "Review redaction",
                "processor-1");

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(processingResultRepository.findByDocumentId(document.getId())).thenReturn(Optional.empty());

        when(processingResultRepository.save(any(DocumentProcessingResult.class))).thenAnswer(invocation -> invocation.getArgument(
                0));

        DocumentProcessingResultResponse response = service.completeExtraction(document.getId(), extractionResponse);

        assertEquals(DocumentStatus.READY_FOR_DEIDENTIFICATION_REVIEW, document.getStatus());

        assertEquals("Machine de-identified text", response.machineDeidentifiedText());

        assertEquals("Review redaction", response.processingWarning());
    }

    @Test
    void shouldRejectExtractionFromInvalidDocumentState() {
        UUID userId = UUID.randomUUID();

        Document document = document(DocumentType.APPOINTMENT_LETTER, DocumentStatus.ACCEPTED);

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(lifecyclePolicy.isStale(any(Document.class), any())).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.beginExtraction(userId, document.getId()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void shouldApproveDeidentifiedTextBeforeSummarisation() {
        UUID userId = UUID.randomUUID();

        Document document = document(
                DocumentType.CONSULTATION_OUTCOME_LETTER,
                DocumentStatus.READY_FOR_DEIDENTIFICATION_REVIEW);

        DocumentProcessingResult result = processingResult(document);

        User reviewer = user(userId);

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(lifecyclePolicy.isStale(any(Document.class), any())).thenReturn(false);

        when(processingRecordService.requireProcessingResult(document.getId())).thenReturn(result);

        when(entityManager.getReference(User.class, userId)).thenReturn(reviewer);

        DocumentSummarisationContext context = service.beginSummarisation(
                userId,
                document.getId(),
                "Approved de-identified text");

        assertEquals(DocumentStatus.SUMMARISING, document.getStatus());

        assertEquals("Approved de-identified text", result.getApprovedDeidentifiedText());

        assertEquals(reviewer, result.getDeidentificationReviewedBy());

        assertEquals("Approved de-identified text", context.approvedDeidentifiedText());

        ArgumentCaptor<AppEvent> eventCaptor = ArgumentCaptor.forClass(AppEvent.class);

        verify(appEventPublisher).publish(eventCaptor.capture());

        PatientActivityEvent event = (PatientActivityEvent) eventCaptor.getValue();

        assertEquals(PatientActivityAction.DEIDENTIFICATION_APPROVED, event.action());
    }

    @Test
    void shouldRetrySummarisationWithExactApprovedTextSnapshot() {
        UUID userId = UUID.randomUUID();

        Document document = document(DocumentType.CONSULTATION_OUTCOME_LETTER, DocumentStatus.SUMMARISATION_FAILED);

        document.setProcessingFailureReason("previous failure");

        DocumentProcessingResult result = processingResult(document);

        result.setApprovedDeidentifiedText("Previously approved text");

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(lifecyclePolicy.isStale(any(Document.class), any())).thenReturn(false);

        when(processingRecordService.requireProcessingResult(document.getId())).thenReturn(result);

        DocumentSummarisationContext context = service.beginSummarisation(
                userId,
                document.getId(),
                "Previously approved text");

        assertEquals(DocumentStatus.SUMMARISING, document.getStatus());

        assertNull(document.getProcessingFailureReason());

        assertEquals("Previously approved text", context.approvedDeidentifiedText());

        verify(appEventPublisher, never()).publish(any());
    }

    @Test
    void shouldRejectSummarisationForAppointmentLetter() {
        UUID userId = UUID.randomUUID();

        Document document = document(DocumentType.APPOINTMENT_LETTER, DocumentStatus.READY_FOR_DEIDENTIFICATION_REVIEW);

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.beginSummarisation(userId, document.getId(), "Approved text"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(processingRecordService, never()).requireProcessingResult(any());
    }

    @Test
    void shouldRejectSummarisationFromInvalidDocumentState() {
        UUID userId = UUID.randomUUID();

        Document document = document(DocumentType.CONSULTATION_OUTCOME_LETTER, DocumentStatus.ACCEPTED);

        DocumentProcessingResult result = processingResult(document);

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(lifecyclePolicy.isStale(any(Document.class), any())).thenReturn(false);

        when(processingRecordService.requireProcessingResult(document.getId())).thenReturn(result);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.beginSummarisation(userId, document.getId(), "Approved text"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void shouldRejectBlankApprovedTextAtServiceBoundary() {
        UUID userId = UUID.randomUUID();

        Document document = document(
                DocumentType.CONSULTATION_OUTCOME_LETTER,
                DocumentStatus.READY_FOR_DEIDENTIFICATION_REVIEW);

        DocumentProcessingResult result = processingResult(document);

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(lifecyclePolicy.isStale(any(Document.class), any())).thenReturn(false);

        when(processingRecordService.requireProcessingResult(document.getId())).thenReturn(result);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.beginSummarisation(userId, document.getId(), "   "));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(appEventPublisher, never()).publish(any());
    }

    @Test
    void shouldRejectChangedApprovedTextDuringSummarisationRetry() {
        UUID userId = UUID.randomUUID();

        Document document = document(DocumentType.CONSULTATION_OUTCOME_LETTER, DocumentStatus.SUMMARISATION_FAILED);

        DocumentProcessingResult result = processingResult(document);

        result.setApprovedDeidentifiedText("Previously approved text");

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(lifecyclePolicy.isStale(any(Document.class), any())).thenReturn(false);

        when(processingRecordService.requireProcessingResult(document.getId())).thenReturn(result);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.beginSummarisation(userId, document.getId(), "Changed text"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void shouldCompleteSummarisationReadyForSummaryReview() {
        Document document = document(DocumentType.CONSULTATION_OUTCOME_LETTER, DocumentStatus.SUMMARISING);

        DocumentProcessingResult result = processingResult(document);

        result.setApprovedDeidentifiedText("Approved text");

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(processingRecordService.requireProcessingResult(document.getId())).thenReturn(result);

        DocumentProcessingResultResponse response = service.completeSummarisation(
                document.getId(),
                new DocumentSummaryResponse(
                        document.getId(),
                        "Generated summary",
                        "processor-2",
                        "model-a",
                        "prompt-v1"));

        assertEquals(DocumentStatus.READY_FOR_SUMMARY_REVIEW, document.getStatus());

        assertEquals("Generated summary", response.generatedSummary());

        assertEquals(SummarySource.OPENAI, response.summarySource());

        assertEquals("model-a", response.model().name());
    }

    @Test
    void shouldRecoverStaleExtractionBeforeRetry() {
        UUID userId = UUID.randomUUID();

        Document document = document(DocumentType.APPOINTMENT_LETTER, DocumentStatus.EXTRACTING);

        document.setProcessingFailureReason("old failure");

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(lifecyclePolicy.isStale(any(Document.class), any())).thenReturn(true);

        service.beginExtraction(userId, document.getId());

        assertEquals(DocumentStatus.EXTRACTING, document.getStatus());

        assertNull(document.getProcessingFailureReason());
    }

    private DocumentProcessingResult processingResult(
            Document document) {
        DocumentProcessingResult result = new DocumentProcessingResult();

        result.setDocument(document);
        result.setExtractedText("Extracted text");
        result.setMachineDeidentifiedText("Machine text");
        result.setProcessorVersion("processor-1");

        return result;
    }

    private Document document(DocumentType type, DocumentStatus status) {
        Document document = new Document();

        ReflectionTestUtils.setField(document, "id", UUID.randomUUID());

        document.setPatientRecord(patientRecord(UUID.randomUUID()));

        document.setDocumentType(type);
        document.setStatus(status);
        document.setOriginalFileName("letter.pdf");
        document.setContentType("application/pdf");
        document.setStoragePath("ab/letter.pdf");

        return document;
    }

    private PatientRecord patientRecord(UUID id) {
        PatientRecord patientRecord = new PatientRecord();

        ReflectionTestUtils.setField(patientRecord, "id", id);

        return patientRecord;
    }

    private User user(UUID id) {
        User user = new User();

        ReflectionTestUtils.setField(user, "id", id);

        return user;
    }
}