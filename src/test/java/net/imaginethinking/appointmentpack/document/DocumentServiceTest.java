package net.imaginethinking.appointmentpack.document;

import net.imaginethinking.appointmentpack.document.storage.DocumentStorageService;
import net.imaginethinking.appointmentpack.event.AppEvent;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import net.imaginethinking.appointmentpack.user.User;
import net.imaginethinking.appointmentpack.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientRecordAccessService patientRecordAccessService;

    @Mock
    private DocumentFileValidator documentFileValidator;

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private AppEventPublisher appEventPublisher;

    private DocumentService service;

    @BeforeEach
    void setUp() {
        service = new DocumentService(
                documentRepository,
                userRepository,
                patientRecordAccessService,
                documentFileValidator,
                documentStorageService,
                appEventPublisher);
    }

    @Test
    void shouldUploadDocumentUsingValidatedContentType() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();
        PatientRecord patientRecord = patientRecord(patientRecordId);
        User user = user(userId);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "consultation.exe",
                "application/octet-stream",
                new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D});

        when(patientRecordAccessService.requireAccess(userId, patientRecordId, DocumentPermission.UPLOAD)).thenReturn(
                patientRecord);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(documentFileValidator.validateAndGetContentType(file)).thenReturn("application/pdf");

        when(documentStorageService.store(file, "application/pdf")).thenReturn(
                "ab/12345678-1234-1234-1234-123456789012.pdf");

        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);

            ReflectionTestUtils.setField(document, "id", UUID.randomUUID());

            return document;
        });

        DocumentResponse response = service.upload(
                userId,
                patientRecordId,
                DocumentType.CONSULTATION_OUTCOME_LETTER,
                file);

        assertNotNull(response.id());
        assertEquals(patientRecordId, response.patientRecordId());

        assertEquals(DocumentType.CONSULTATION_OUTCOME_LETTER, response.documentType());

        assertEquals(DocumentStatus.UPLOADED, response.status());

        assertEquals("consultation.exe", response.originalFileName());

        assertEquals("application/pdf", response.contentType());

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);

        verify(documentRepository).saveAndFlush(documentCaptor.capture());

        assertEquals("12345678-1234-1234-1234-123456789012.pdf", documentCaptor.getValue().getStoredFileName());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldRejectUploadWithoutDocumentType() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.upload(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        new MockMultipartFile("file", "letter.pdf", "application/pdf", new byte[]{1})));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(patientRecordAccessService, never()).requireAccess(any(), any(UUID.class), any());
    }

    @Test
    void shouldDeleteStoredFileWhenDocumentPersistenceFails() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        PatientRecord patientRecord = patientRecord(patientRecordId);

        MockMultipartFile file = new MockMultipartFile("file", "letter.pdf", "application/pdf", new byte[]{1});

        when(patientRecordAccessService.requireAccess(userId, patientRecordId, DocumentPermission.UPLOAD)).thenReturn(
                patientRecord);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));

        when(documentFileValidator.validateAndGetContentType(file)).thenReturn("application/pdf");

        when(documentStorageService.store(file, "application/pdf")).thenReturn("ab/document.pdf");

        RuntimeException persistenceFailure = new RuntimeException("database failure");

        when(documentRepository.saveAndFlush(any(Document.class))).thenThrow(persistenceFailure);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> service.upload(userId, patientRecordId, DocumentType.APPOINTMENT_LETTER, file));

        assertEquals(persistenceFailure, thrown);

        verify(documentStorageService).delete("ab/document.pdf");
    }

    @Test
    void shouldDownloadAvailableDocumentAndPublishAuditEvent() {
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        Document document = document(documentId, DocumentStatus.ACCEPTED);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        when(documentStorageService.load(document.getStoragePath())).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));

        DocumentDownload download = service.download(userId, documentId);

        assertEquals(document.getOriginalFileName(), download.fileName());

        assertEquals(document.getContentType(), download.contentType());

        ArgumentCaptor<AppEvent> eventCaptor = ArgumentCaptor.forClass(AppEvent.class);

        verify(appEventPublisher).publish(eventCaptor.capture());

        PatientActivityEvent event = (PatientActivityEvent) eventCaptor.getValue();

        assertEquals(PatientActivityAction.DOWNLOADED, event.action());
    }

    @Test
    void shouldAllowAcceptedDocumentToBeArchivedIdempotently() {
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        Document document = document(documentId, DocumentStatus.ACCEPTED);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        DocumentResponse first = service.archive(userId, documentId);

        DocumentResponse second = service.archive(userId, documentId);

        assertEquals(DocumentStatus.ARCHIVED, first.status());

        assertEquals(DocumentStatus.ARCHIVED, second.status());

        verify(appEventPublisher, times(1)).publish(any());
    }

    @Test
    void shouldRejectArchiveWhileDocumentIsProcessing() {
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        Document document = document(documentId, DocumentStatus.EXTRACTING);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.archive(userId, documentId));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(appEventPublisher, never()).publish(any());
    }

    private Document document(UUID id, DocumentStatus status) {
        Document document = new Document();

        ReflectionTestUtils.setField(document, "id", id);

        document.setPatientRecord(patientRecord(UUID.randomUUID()));

        document.setUploadedBy(user(UUID.randomUUID()));

        document.setDocumentType(DocumentType.APPOINTMENT_LETTER);

        document.setStatus(status);
        document.setOriginalFileName("letter.pdf");
        document.setStoredFileName("stored.pdf");
        document.setContentType("application/pdf");
        document.setFileSize(3);
        document.setStoragePath("ab/stored.pdf");

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