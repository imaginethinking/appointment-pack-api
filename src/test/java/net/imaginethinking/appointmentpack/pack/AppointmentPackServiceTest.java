package net.imaginethinking.appointmentpack.pack;

import net.imaginethinking.appointmentpack.appointment.Appointment;
import net.imaginethinking.appointmentpack.event.AppEvent;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.pack.generation.AppointmentPackGenerationData;
import net.imaginethinking.appointmentpack.pack.generation.AppointmentPackGenerationDataService;
import net.imaginethinking.appointmentpack.pack.generation.AppointmentPackPdfRenderer;
import net.imaginethinking.appointmentpack.pack.generation.AppointmentPackRenderModel;
import net.imaginethinking.appointmentpack.pack.storage.AppointmentPackStorageService;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import net.imaginethinking.appointmentpack.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentPackServiceTest {

    @Mock
    private AppointmentPackRepository appointmentPackRepository;

    @Mock
    private PatientRecordAccessService patientRecordAccessService;

    @Mock
    private AppointmentPackStorageService appointmentPackStorageService;

    @Mock
    private AppointmentPackGenerationDataService generationDataService;

    @Mock
    private AppointmentPackPdfRenderer pdfRenderer;

    @Mock
    private AppointmentPackPersistenceService persistenceService;

    @Mock
    private AppEventPublisher appEventPublisher;

    private AppointmentPackService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentPackService(
                appointmentPackRepository,
                patientRecordAccessService,
                appointmentPackStorageService,
                generationDataService,
                pdfRenderer,
                persistenceService,
                appEventPublisher);
    }

    @Test
    void shouldGenerateStoreAndPersistAppointmentPack() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        AppointmentPackGenerationRequest request = request();

        AppointmentPackGenerationData data = generationData(patientRecordId, request.appointmentId());

        byte[] pdf = new byte[]{1, 2, 3, 4};

        AppointmentPackResponse persistedResponse = response(
                UUID.randomUUID(),
                patientRecordId,
                request.appointmentId());

        when(generationDataService.prepare(userId, patientRecordId, request)).thenReturn(data);

        when(pdfRenderer.render(data.renderModel())).thenReturn(pdf);

        when(appointmentPackStorageService.store(pdf)).thenReturn("ab/pack.pdf");

        when(persistenceService.persist(userId, data, "ab/pack.pdf", pdf.length)).thenReturn(persistedResponse);

        AppointmentPackResponse result = service.generateAppointmentPack(userId, patientRecordId, request);

        assertSame(persistedResponse, result);

        verify(generationDataService).prepare(userId, patientRecordId, request);

        verify(pdfRenderer).render(data.renderModel());

        verify(appointmentPackStorageService).store(pdf);

        verify(persistenceService).persist(userId, data, "ab/pack.pdf", pdf.length);
    }

    @Test
    void shouldRejectEmptyGeneratedPdfBeforeStorage() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        AppointmentPackGenerationRequest request = request();

        AppointmentPackGenerationData data = generationData(patientRecordId, request.appointmentId());

        when(generationDataService.prepare(userId, patientRecordId, request)).thenReturn(data);

        when(pdfRenderer.render(data.renderModel())).thenReturn(new byte[0]);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.generateAppointmentPack(userId, patientRecordId, request));

        assertEquals("Generated appointment pack PDF was empty", exception.getMessage());

        verify(appointmentPackStorageService, never()).store(any());

        verify(persistenceService, never()).persist(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void shouldDeleteStoredFileWhenPersistenceFails() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        AppointmentPackGenerationRequest request = request();

        AppointmentPackGenerationData data = generationData(patientRecordId, request.appointmentId());

        byte[] pdf = new byte[]{1, 2, 3};

        RuntimeException persistenceFailure = new RuntimeException("database failure");

        when(generationDataService.prepare(userId, patientRecordId, request)).thenReturn(data);

        when(pdfRenderer.render(data.renderModel())).thenReturn(pdf);

        when(appointmentPackStorageService.store(pdf)).thenReturn("ab/pack.pdf");

        when(persistenceService.persist(userId, data, "ab/pack.pdf", pdf.length)).thenThrow(persistenceFailure);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> service.generateAppointmentPack(userId, patientRecordId, request));

        assertSame(persistenceFailure, thrown);

        verify(appointmentPackStorageService).delete("ab/pack.pdf");
    }

    @Test
    void shouldDownloadAvailablePackAndPublishPatientActivity() {
        UUID userId = UUID.randomUUID();

        AppointmentPack pack = pack(false);

        when(appointmentPackRepository.findById(pack.getId())).thenReturn(Optional.of(pack));

        ByteArrayResource resource = new ByteArrayResource(new byte[]{1, 2, 3});

        when(appointmentPackStorageService.load(pack.getStoragePath())).thenReturn(resource);

        AppointmentPackDownload download = service.downloadAppointmentPack(userId, pack.getId());

        assertSame(resource, download.resource());

        assertEquals(pack.getFileName(), download.fileName());

        assertEquals(pack.getContentType(), download.contentType());

        assertEquals(pack.getFileSize(), download.fileSize());

        verify(patientRecordAccessService).requireAccess(
                userId,
                pack.getPatientRecord(),
                AppointmentPackPermission.VIEW);

        PatientActivityEvent event = capturedActivityEvent();

        assertEquals(PatientActivityAction.DOWNLOADED, event.action());
    }

    @Test
    void shouldArchivePackIdempotently() {
        UUID userId = UUID.randomUUID();

        AppointmentPack pack = pack(false);

        when(appointmentPackRepository.findById(pack.getId())).thenReturn(Optional.of(pack));

        AppointmentPackResponse first = service.archiveAppointmentPack(userId, pack.getId());

        AppointmentPackResponse second = service.archiveAppointmentPack(userId, pack.getId());

        assertNotNull(first.archivedAt());

        assertEquals(first.archivedAt(), second.archivedAt());

        verify(patientRecordAccessService, times(2)).requireAccess(
                userId,
                pack.getPatientRecord(),
                AppointmentPackPermission.CREATE);

        verify(appEventPublisher, times(1)).publish(any());
    }

    @Test
    void shouldHideArchivedPackFromNormalRetrieval() {
        UUID userId = UUID.randomUUID();

        AppointmentPack pack = pack(true);

        when(appointmentPackRepository.findById(pack.getId())).thenReturn(Optional.of(pack));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getAppointmentPack(userId, pack.getId()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(patientRecordAccessService, never()).requireAccess(any(), any(PatientRecord.class), any());
    }

    private PatientActivityEvent capturedActivityEvent() {
        ArgumentCaptor<AppEvent> eventCaptor = ArgumentCaptor.forClass(AppEvent.class);

        verify(appEventPublisher).publish(eventCaptor.capture());

        return (PatientActivityEvent) eventCaptor.getValue();
    }

    private AppointmentPackGenerationRequest request() {
        return new AppointmentPackGenerationRequest(
                UUID.randomUUID(),
                "Pack",
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    private AppointmentPackGenerationData generationData(UUID patientRecordId, UUID appointmentId) {
        return new AppointmentPackGenerationData(
                patientRecordId,
                appointmentId,
                "Pack",
                null,
                Instant.parse("2026-08-14T12:00:00Z"),
                "pack.pdf",
                renderModel(),
                List.of());
    }

    private AppointmentPackRenderModel renderModel() {
        return new AppointmentPackRenderModel(
                "Pack",
                "14 August 2026",
                new AppointmentPackRenderModel.PatientInformation(
                        "Patient User",
                        "1 January 1990",
                        null,
                        null,
                        null,
                        null,
                        List.of()),
                new AppointmentPackRenderModel.AppointmentInformation(
                        "10 September 2026",
                        "10:00",
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        null),
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    private AppointmentPackResponse response(UUID packId, UUID patientRecordId, UUID appointmentId) {
        return new AppointmentPackResponse(
                packId,
                patientRecordId,
                appointmentId,
                "Pack",
                null,
                UUID.randomUUID(),
                Instant.parse("2026-08-14T12:00:00Z"),
                "pack.pdf",
                4,
                null,
                List.of());
    }

    private AppointmentPack pack(
            boolean archived) {
        PatientRecord patientRecord = new PatientRecord();

        ReflectionTestUtils.setField(patientRecord, "id", UUID.randomUUID());

        Appointment appointment = new Appointment();

        ReflectionTestUtils.setField(appointment, "id", UUID.randomUUID());

        appointment.setPatientRecord(patientRecord);

        User user = new User();

        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        AppointmentPack pack = new AppointmentPack();

        ReflectionTestUtils.setField(pack, "id", UUID.randomUUID());

        pack.setPatientRecord(patientRecord);

        pack.setAppointment(appointment);

        pack.setTitle("Pack");
        pack.setGeneratedBy(user);

        pack.setGeneratedAt(Instant.parse("2026-08-14T12:00:00Z"));

        pack.setFileName("pack.pdf");

        pack.setStoredFileName("stored.pdf");

        pack.setStoragePath("ab/stored.pdf");

        pack.setContentType("application/pdf");

        pack.setFileSize(100);

        if (archived) {
            pack.archive();
        }

        return pack;
    }
}