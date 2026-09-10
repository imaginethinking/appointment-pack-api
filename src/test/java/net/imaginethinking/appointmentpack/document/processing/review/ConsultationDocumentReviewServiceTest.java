package net.imaginethinking.appointmentpack.document.processing.review;

import jakarta.persistence.EntityManager;
import net.imaginethinking.appointmentpack.document.Document;
import net.imaginethinking.appointmentpack.document.DocumentPermission;
import net.imaginethinking.appointmentpack.document.DocumentStatus;
import net.imaginethinking.appointmentpack.document.DocumentType;
import net.imaginethinking.appointmentpack.document.processing.DocumentProcessingRecordService;
import net.imaginethinking.appointmentpack.document.processing.DocumentProcessingResult;
import net.imaginethinking.appointmentpack.document.processing.DocumentProcessingResultMapper;
import net.imaginethinking.appointmentpack.document.processing.SummarySource;
import net.imaginethinking.appointmentpack.document.processing.api.DocumentProcessingResultResponse;
import net.imaginethinking.appointmentpack.document.processing.api.DocumentSummaryAcceptanceRequest;
import net.imaginethinking.appointmentpack.event.AppEvent;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryEntry;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryEntryRepository;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryPermission;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistorySourceType;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Checks consultation document review service behaviour across normal and failure cases.
 */
@ExtendWith(MockitoExtension.class)
class ConsultationDocumentReviewServiceTest {

    @Mock
    private DocumentProcessingRecordService processingRecordService;

    @Mock
    private MedicalHistoryEntryRepository medicalHistoryEntryRepository;

    @Mock
    private PatientRecordAccessService patientRecordAccessService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private AppEventPublisher appEventPublisher;

    private ConsultationDocumentReviewService service;

    /**
     * Creates the common fixtures and mocks used by each test.
     */
    @BeforeEach
    void setUp() {
        service = new ConsultationDocumentReviewService(
                processingRecordService,
                medicalHistoryEntryRepository,
                patientRecordAccessService,
                new DocumentProcessingResultMapper(),
                entityManager,
                appEventPublisher);
    }

    @Test
    void shouldAcceptReviewedGeneratedSummaryIntoMedicalHistory() {
        UUID userId = UUID.randomUUID();

        Document document = consultationDocument(DocumentStatus.READY_FOR_SUMMARY_REVIEW);

        DocumentProcessingResult result = processingResult(document);

        result.setGeneratedSummary("AI generated summary");

        result.setSummarySource(SummarySource.OPENAI);

        result.setModelName("model-a");
        result.setPromptVersion("prompt-v1");

        User reviewer = user(userId);

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(medicalHistoryEntryRepository.existsBySourceDocumentId(document.getId())).thenReturn(false);

        when(processingRecordService.requireProcessingResult(document.getId())).thenReturn(result);

        when(entityManager.getReference(User.class, userId)).thenReturn(reviewer);

        DocumentProcessingResultResponse response = service.acceptSummary(
                userId,
                document.getId(),
                new DocumentSummaryAcceptanceRequest(
                        " Reviewed summary ",
                        " Neurology consultation ",
                        LocalDate.of(2026, 8, 5)));

        assertEquals(DocumentStatus.ACCEPTED, document.getStatus());

        assertEquals("Reviewed summary", result.getReviewedSummary());

        assertEquals(reviewer, result.getSummaryReviewedBy());

        assertNotNull(result.getSummaryReviewedAt());

        assertEquals(DocumentStatus.ACCEPTED, response.status());

        ArgumentCaptor<MedicalHistoryEntry> historyCaptor = ArgumentCaptor.forClass(MedicalHistoryEntry.class);

        verify(medicalHistoryEntryRepository).save(historyCaptor.capture());

        MedicalHistoryEntry historyEntry = historyCaptor.getValue();

        assertEquals("Neurology consultation", historyEntry.getTitle());

        assertEquals("Reviewed summary", historyEntry.getSummary());

        assertEquals(MedicalHistorySourceType.DOCUMENT_SUMMARY, historyEntry.getSourceType());

        assertEquals(document, historyEntry.getSourceDocument());

        assertEquals(reviewer, historyEntry.getCreatedBy());

        verify(patientRecordAccessService).requireAccess(userId, document.getPatientRecord(), DocumentPermission.EDIT);

        verify(patientRecordAccessService).requireAccess(
                userId,
                document.getPatientRecord(),
                MedicalHistoryPermission.EDIT);

        PatientActivityEvent event = capturedActivityEvent();

        assertEquals(PatientActivityAction.SUMMARY_ACCEPTED, event.action());
    }

    @Test
    void shouldAcceptManualSummaryAfterSummarisationFailure() {
        UUID userId = UUID.randomUUID();

        Document document = consultationDocument(DocumentStatus.SUMMARISATION_FAILED);

        DocumentProcessingResult result = processingResult(document);

        result.setGeneratedSummary("stale summary");

        result.setSummarySource(SummarySource.OPENAI);

        result.setModelName("model-a");
        result.setPromptVersion("prompt-v1");

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(medicalHistoryEntryRepository.existsBySourceDocumentId(document.getId())).thenReturn(false);

        when(processingRecordService.requireProcessingResult(document.getId())).thenReturn(result);

        when(entityManager.getReference(User.class, userId)).thenReturn(user(userId));

        DocumentProcessingResultResponse response = service.acceptSummary(
                userId,
                document.getId(),
                new DocumentSummaryAcceptanceRequest(
                        "Manual reviewed summary",
                        "Consultation",
                        LocalDate.of(2026, 8, 5)));

        assertEquals(DocumentStatus.ACCEPTED, response.status());

        assertEquals(SummarySource.MANUAL, result.getSummarySource());

        assertNull(result.getGeneratedSummary());

        assertNull(result.getModelName());

        assertNull(result.getPromptVersion());

        assertEquals("Manual reviewed summary", result.getReviewedSummary());
    }

    @Test
    void shouldRejectDuplicateMedicalHistoryForSourceDocument() {
        UUID userId = UUID.randomUUID();

        Document document = consultationDocument(DocumentStatus.READY_FOR_SUMMARY_REVIEW);

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(medicalHistoryEntryRepository.existsBySourceDocumentId(document.getId())).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.acceptSummary(
                        userId,
                        document.getId(),
                        new DocumentSummaryAcceptanceRequest("Summary", "History", LocalDate.of(2026, 8, 5))));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(processingRecordService, never()).requireProcessingResult(any());
    }

    @Test
    void shouldRejectSummaryAndMarkDocumentRejected() {
        UUID userId = UUID.randomUUID();

        Document document = consultationDocument(DocumentStatus.READY_FOR_SUMMARY_REVIEW);

        DocumentProcessingResult result = processingResult(document);

        result.setGeneratedSummary("Generated summary");

        result.setSummarySource(SummarySource.OPENAI);

        User reviewer = user(userId);

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(processingRecordService.requireProcessingResult(document.getId())).thenReturn(result);

        when(entityManager.getReference(User.class, userId)).thenReturn(reviewer);

        DocumentProcessingResultResponse response = service.rejectSummary(userId, document.getId());

        assertEquals(DocumentStatus.REJECTED, document.getStatus());

        assertEquals(DocumentStatus.REJECTED, response.status());

        assertEquals(userId, response.summaryReviewedByUserId());

        assertNotNull(response.summaryReviewedAt());

        assertNull(response.reviewedSummary());

        PatientActivityEvent event = capturedActivityEvent();

        assertEquals(PatientActivityAction.SUMMARY_REJECTED, event.action());
    }

    /**
     * Returns the patient activity event captured during the test.
     */
    private PatientActivityEvent capturedActivityEvent() {
        ArgumentCaptor<AppEvent> eventCaptor = ArgumentCaptor.forClass(AppEvent.class);

        verify(appEventPublisher).publish(eventCaptor.capture());

        return (PatientActivityEvent) eventCaptor.getValue();
    }

    /**
     * Creates a test consultation document with the supplied values.
     */
    private Document consultationDocument(
            DocumentStatus status) {
        Document document = new Document();

        ReflectionTestUtils.setField(document, "id", UUID.randomUUID());

        document.setPatientRecord(patientRecord());

        document.setDocumentType(DocumentType.CONSULTATION_OUTCOME_LETTER);

        document.setStatus(status);

        return document;
    }

    /**
     * Creates a test processing result with the supplied values.
     */
    private DocumentProcessingResult processingResult(
            Document document) {
        DocumentProcessingResult result = new DocumentProcessingResult();

        result.setDocument(document);
        result.setExtractedText("Extracted text");
        result.setMachineDeidentifiedText("Machine text");
        result.setApprovedDeidentifiedText("Approved text");
        result.setProcessorVersion("processor-1");

        return result;
    }

    /**
     * Creates a test patient record with the standard values used by these tests.
     */
    private PatientRecord patientRecord() {
        PatientRecord patientRecord = new PatientRecord();

        ReflectionTestUtils.setField(patientRecord, "id", UUID.randomUUID());

        return patientRecord;
    }

    /**
     * Creates a test user with the supplied values.
     */
    private User user(UUID id) {
        User user = new User();

        ReflectionTestUtils.setField(user, "id", id);

        return user;
    }
}