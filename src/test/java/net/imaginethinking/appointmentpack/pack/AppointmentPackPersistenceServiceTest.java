package net.imaginethinking.appointmentpack.pack;

import jakarta.persistence.EntityManager;
import net.imaginethinking.appointmentpack.appointment.Appointment;
import net.imaginethinking.appointmentpack.event.AppEvent;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.pack.generation.AppointmentPackGenerationData;
import net.imaginethinking.appointmentpack.pack.generation.AppointmentPackRenderModel;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentPackPersistenceServiceTest {

    @Mock
    private AppointmentPackRepository appointmentPackRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private AppEventPublisher appEventPublisher;

    private AppointmentPackPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentPackPersistenceService(appointmentPackRepository, entityManager, appEventPublisher);
    }

    @Test
    void shouldPersistPackMetadataAndImmutableSelectedItemSnapshot() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID medicationId = UUID.randomUUID();
        UUID bloodTestId = UUID.randomUUID();
        UUID packId = UUID.randomUUID();

        PatientRecord patientRecord = patientRecord(patientRecordId);

        Appointment appointment = appointment(appointmentId);

        User user = user(userId);

        AppointmentPackGenerationData data = generationData(
                patientRecordId, appointmentId, List.of(
                        new AppointmentPackGenerationData.SelectedItem(
                                AppointmentPackItemType.MEDICATION,
                                medicationId,
                                0),
                        new AppointmentPackGenerationData.SelectedItem(
                                AppointmentPackItemType.BLOOD_TEST,
                                bloodTestId,
                                1)));

        when(entityManager.getReference(PatientRecord.class, patientRecordId)).thenReturn(patientRecord);

        when(entityManager.getReference(Appointment.class, appointmentId)).thenReturn(appointment);

        when(entityManager.getReference(User.class, userId)).thenReturn(user);

        when(appointmentPackRepository.saveAndFlush(any(AppointmentPack.class))).thenAnswer(invocation -> {
            AppointmentPack pack = invocation.getArgument(0);

            ReflectionTestUtils.setField(pack, "id", packId);

            return pack;
        });

        AppointmentPackResponse response = service.persist(userId, data, "ab/generated-pack.pdf", 2048);

        assertEquals(packId, response.id());

        assertEquals(patientRecordId, response.patientRecordId());

        assertEquals(appointmentId, response.appointmentId());

        assertEquals("Neurology Pack", response.title());

        assertEquals("Pack notes", response.notes());

        assertEquals(userId, response.generatedByUserId());

        assertEquals("neurology-pack.pdf", response.fileName());

        assertEquals(2048, response.fileSize());

        assertEquals(
                List.of(AppointmentPackItemType.MEDICATION, AppointmentPackItemType.BLOOD_TEST),
                response.items().stream().map(AppointmentPackResponse.ItemResponse::resourceType).toList());

        ArgumentCaptor<AppointmentPack> packCaptor = ArgumentCaptor.forClass(AppointmentPack.class);

        verify(appointmentPackRepository).saveAndFlush(packCaptor.capture());

        AppointmentPack persisted = packCaptor.getValue();

        assertEquals("generated-pack.pdf", persisted.getStoredFileName());

        assertEquals("application/pdf", persisted.getContentType());

        assertEquals(2, persisted.getItems().size());

        assertEquals(persisted, persisted.getItems().getFirst().getAppointmentPack());
    }

    @Test
    void shouldPublishGeneratedPatientActivityEvent() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID packId = UUID.randomUUID();

        when(entityManager.getReference(
                PatientRecord.class,
                patientRecordId)).thenReturn(patientRecord(patientRecordId));

        when(entityManager.getReference(Appointment.class, appointmentId)).thenReturn(appointment(appointmentId));

        when(entityManager.getReference(User.class, userId)).thenReturn(user(userId));

        when(appointmentPackRepository.saveAndFlush(any(AppointmentPack.class))).thenAnswer(invocation -> {
            AppointmentPack pack = invocation.getArgument(0);

            ReflectionTestUtils.setField(pack, "id", packId);

            return pack;
        });

        service.persist(
                userId,
                generationData(patientRecordId, appointmentId, List.of()),
                "ab/generated-pack.pdf",
                100);

        ArgumentCaptor<AppEvent> eventCaptor = ArgumentCaptor.forClass(AppEvent.class);

        verify(appEventPublisher).publish(eventCaptor.capture());

        PatientActivityEvent event = (PatientActivityEvent) eventCaptor.getValue();

        assertEquals(PatientActivityAction.GENERATED, event.action());

        assertEquals(patientRecordId, event.patientRecordId());

        assertEquals(packId, event.resourceId());
    }

    private AppointmentPackGenerationData generationData(
            UUID patientRecordId,
            UUID appointmentId,
            List<AppointmentPackGenerationData.SelectedItem> selectedItems) {
        return new AppointmentPackGenerationData(
                patientRecordId,
                appointmentId,
                "Neurology Pack",
                "Pack notes",
                Instant.parse("2026-08-14T12:00:00Z"),
                "neurology-pack.pdf",
                renderModel(),
                selectedItems);
    }

    private AppointmentPackRenderModel renderModel() {
        return new AppointmentPackRenderModel(
                "Neurology Pack",
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
                "Pack notes",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    private PatientRecord patientRecord(
            UUID id) {
        PatientRecord patientRecord = new PatientRecord();

        ReflectionTestUtils.setField(patientRecord, "id", id);

        return patientRecord;
    }

    private Appointment appointment(
            UUID id) {
        Appointment appointment = new Appointment();

        ReflectionTestUtils.setField(appointment, "id", id);

        return appointment;
    }

    private User user(UUID id) {
        User user = new User();

        ReflectionTestUtils.setField(user, "id", id);

        return user;
    }
}