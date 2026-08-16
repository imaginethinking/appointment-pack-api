package net.imaginethinking.appointmentpack.document.processing.review;

import jakarta.persistence.EntityManager;
import net.imaginethinking.appointmentpack.appointment.*;
import net.imaginethinking.appointmentpack.document.Document;
import net.imaginethinking.appointmentpack.document.DocumentPermission;
import net.imaginethinking.appointmentpack.document.DocumentStatus;
import net.imaginethinking.appointmentpack.document.DocumentType;
import net.imaginethinking.appointmentpack.document.processing.DocumentProcessingRecordService;
import net.imaginethinking.appointmentpack.document.processing.DocumentProcessingResult;
import net.imaginethinking.appointmentpack.document.processing.DocumentProcessingResultMapper;
import net.imaginethinking.appointmentpack.document.processing.api.DocumentProcessingResultResponse;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentDocumentReviewServiceTest {

    @Mock
    private DocumentProcessingRecordService processingRecordService;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRecordAccessService patientRecordAccessService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private AppEventPublisher appEventPublisher;

    private AppointmentDocumentReviewService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentDocumentReviewService(
                processingRecordService,
                appointmentRepository,
                patientRecordAccessService,
                new DocumentProcessingResultMapper(),
                entityManager,
                appEventPublisher);
    }

    @Test
    void shouldConfirmAppointmentAndAcceptSourceDocument() {
        UUID userId = UUID.randomUUID();

        Document document = appointmentDocument(DocumentStatus.READY_FOR_APPOINTMENT_REVIEW);

        DocumentProcessingResult result = processingResult(document);

        User reviewer = user(userId);

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(appointmentRepository.existsBySourceDocument_Id(document.getId())).thenReturn(false);

        when(processingRecordService.requireProcessingResult(document.getId())).thenReturn(result);

        when(entityManager.getReference(User.class, userId)).thenReturn(reviewer);

        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);

            ReflectionTestUtils.setField(appointment, "id", UUID.randomUUID());

            return appointment;
        });

        AppointmentResponse response = service.confirmAppointment(
                userId, document.getId(), new AppointmentConfirmationRequest(
                        LocalDate.of(2026, 9, 10),
                        LocalTime.of(10, 0),
                        LocalTime.of(10, 30),
                        " Neurology ",
                        " Follow-up ",
                        " Dr Smith ",
                        " Clinic A ",
                        null,
                        " Bring notes "));

        assertNotNull(response.id());

        assertEquals(document.getId(), response.sourceDocumentId());

        assertEquals("Neurology", response.service());

        assertEquals("Bring notes", response.notes());

        assertEquals(DocumentStatus.ACCEPTED, document.getStatus());

        assertEquals(reviewer, result.getAppointmentReviewedBy());

        assertNotNull(result.getAppointmentReviewedAt());

        verify(patientRecordAccessService).requireAccess(userId, document.getPatientRecord(), DocumentPermission.EDIT);

        verify(patientRecordAccessService).requireAccess(
                userId,
                document.getPatientRecord(),
                AppointmentPermission.EDIT);

        PatientActivityEvent event = capturedActivityEvent();

        assertEquals(PatientActivityAction.APPOINTMENT_CONFIRMED, event.action());
    }

    @Test
    void shouldRejectDuplicateAppointmentForSameSourceDocument() {
        UUID userId = UUID.randomUUID();

        Document document = appointmentDocument(DocumentStatus.READY_FOR_APPOINTMENT_REVIEW);

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(appointmentRepository.existsBySourceDocument_Id(document.getId())).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.confirmAppointment(userId, document.getId(), validRequest()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(processingRecordService, never()).requireProcessingResult(any());
    }

    @Test
    void shouldRejectInvalidAppointmentTimes() {
        UUID userId = UUID.randomUUID();

        Document document = appointmentDocument(DocumentStatus.READY_FOR_APPOINTMENT_REVIEW);

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(appointmentRepository.existsBySourceDocument_Id(document.getId())).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.confirmAppointment(
                        userId, document.getId(), new AppointmentConfirmationRequest(
                                LocalDate.of(2026, 9, 10),
                                LocalTime.of(10, 0),
                                LocalTime.of(9, 30),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldRejectExtractedAppointmentAndMarkDocumentRejected() {
        UUID userId = UUID.randomUUID();

        Document document = appointmentDocument(DocumentStatus.READY_FOR_APPOINTMENT_REVIEW);

        DocumentProcessingResult result = processingResult(document);

        User reviewer = user(userId);

        when(processingRecordService.requireAvailableDocument(document.getId())).thenReturn(document);

        when(processingRecordService.requireProcessingResult(document.getId())).thenReturn(result);

        when(entityManager.getReference(User.class, userId)).thenReturn(reviewer);

        DocumentProcessingResultResponse response = service.rejectAppointment(userId, document.getId());

        assertEquals(DocumentStatus.REJECTED, document.getStatus());

        assertEquals(DocumentStatus.REJECTED, response.status());

        assertEquals(userId, response.appointmentReviewedByUserId());

        assertNotNull(response.appointmentReviewedAt());

        PatientActivityEvent event = capturedActivityEvent();

        assertEquals(PatientActivityAction.APPOINTMENT_REJECTED, event.action());
    }

    private AppointmentConfirmationRequest validRequest() {
        return new AppointmentConfirmationRequest(
                LocalDate.of(2026, 9, 10),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30),
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private PatientActivityEvent capturedActivityEvent() {
        ArgumentCaptor<AppEvent> eventCaptor = ArgumentCaptor.forClass(AppEvent.class);

        verify(appEventPublisher).publish(eventCaptor.capture());

        return (PatientActivityEvent) eventCaptor.getValue();
    }

    private Document appointmentDocument(
            DocumentStatus status) {
        Document document = new Document();

        ReflectionTestUtils.setField(document, "id", UUID.randomUUID());

        document.setPatientRecord(patientRecord());

        document.setDocumentType(DocumentType.APPOINTMENT_LETTER);

        document.setStatus(status);

        return document;
    }

    private DocumentProcessingResult processingResult(
            Document document) {
        DocumentProcessingResult result = new DocumentProcessingResult();

        result.setDocument(document);
        result.setExtractedText("Extracted text");
        result.setProcessorVersion("processor-1");

        return result;
    }

    private PatientRecord patientRecord() {
        PatientRecord patientRecord = new PatientRecord();

        ReflectionTestUtils.setField(patientRecord, "id", UUID.randomUUID());

        return patientRecord;
    }

    private User user(UUID id) {
        User user = new User();

        ReflectionTestUtils.setField(user, "id", id);

        return user;
    }
}