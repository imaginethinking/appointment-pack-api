package net.imaginethinking.appointmentpack.pack.generation;

import net.imaginethinking.appointmentpack.appointment.Appointment;
import net.imaginethinking.appointmentpack.pack.AppointmentPackGenerationRequest;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks appointment pack generation data service behaviour across normal and failure cases.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentPackGenerationDataServiceTest {

    @Mock
    private AppointmentPackSelectionService selectionService;

    @Mock
    private AppointmentPackRenderModelFactory renderModelFactory;

    private AppointmentPackGenerationDataService service;

    /**
     * Creates the common fixtures and mocks used by each test.
     */
    @BeforeEach
    void setUp() {
        service = new AppointmentPackGenerationDataService(selectionService, renderModelFactory);
    }

    @Test
    void shouldNormaliseRequestedTitleAndNotesAndCreateSafeFilename() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        Appointment appointment = appointment(patientRecord(patientRecordId), "Neurology");

        AppointmentPackSelection selection = selection(appointment);

        AppointmentPackGenerationRequest request = new AppointmentPackGenerationRequest(
                appointment.getId(),
                "  Résumé & Neurology Pack!  ",
                "  Bring medication list  ",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        AppointmentPackRenderModel renderModel = renderModel();

        when(selectionService.select(userId, patientRecordId, request)).thenReturn(selection);

        when(renderModelFactory.create(
                org.mockito.ArgumentMatchers.eq(selection),
                org.mockito.ArgumentMatchers.eq("Résumé & Neurology Pack!"),
                org.mockito.ArgumentMatchers.eq("Bring medication list"),
                org.mockito.ArgumentMatchers.any(Instant.class))).thenReturn(renderModel);

        AppointmentPackGenerationData data = service.prepare(userId, patientRecordId, request);

        assertEquals("Résumé & Neurology Pack!", data.title());

        assertEquals("Bring medication list", data.notes());

        assertEquals("resume-neurology-pack.pdf", data.fileName());

        assertEquals(renderModel, data.renderModel());

        assertNotNull(data.generatedAt());
    }

    @Test
    void shouldGenerateDefaultTitleFromAppointmentServiceAndDate() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        Appointment appointment = appointment(patientRecord(patientRecordId), " Neurology ");

        AppointmentPackSelection selection = selection(appointment);

        AppointmentPackGenerationRequest request = emptyRequest(appointment.getId());

        when(selectionService.select(userId, patientRecordId, request)).thenReturn(selection);

        when(renderModelFactory.create(
                org.mockito.ArgumentMatchers.eq(selection),
                org.mockito.ArgumentMatchers.eq("Neurology Appointment Pack – 10 September 2026"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(Instant.class))).thenReturn(renderModel());

        AppointmentPackGenerationData data = service.prepare(userId, patientRecordId, request);

        assertEquals("Neurology Appointment Pack – 10 September 2026", data.title());

        assertEquals("neurology-appointment-pack-10-september-2026.pdf", data.fileName());
    }

    @Test
    void shouldUseGenericDefaultTitleWhenAppointmentServiceIsBlank() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        Appointment appointment = appointment(patientRecord(patientRecordId), "   ");

        AppointmentPackSelection selection = selection(appointment);

        AppointmentPackGenerationRequest request = emptyRequest(appointment.getId());

        when(selectionService.select(userId, patientRecordId, request)).thenReturn(selection);

        when(renderModelFactory.create(
                org.mockito.ArgumentMatchers.eq(selection),
                org.mockito.ArgumentMatchers.eq("Appointment Pack – 10 September 2026"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(Instant.class))).thenReturn(renderModel());

        AppointmentPackGenerationData data = service.prepare(userId, patientRecordId, request);

        assertEquals("Appointment Pack – 10 September 2026", data.title());

        assertEquals("appointment-pack-10-september-2026.pdf", data.fileName());
    }

    @Test
    void shouldPassSameGenerationTimestampIntoRenderModelFactory() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        Appointment appointment = appointment(patientRecord(patientRecordId), "Neurology");

        AppointmentPackSelection selection = selection(appointment);

        AppointmentPackGenerationRequest request = emptyRequest(appointment.getId());

        when(selectionService.select(userId, patientRecordId, request)).thenReturn(selection);

        when(renderModelFactory.create(
                org.mockito.ArgumentMatchers.eq(selection),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(Instant.class))).thenReturn(renderModel());

        AppointmentPackGenerationData data = service.prepare(userId, patientRecordId, request);

        ArgumentCaptor<Instant> generatedAtCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(renderModelFactory).create(
                org.mockito.ArgumentMatchers.eq(selection),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.isNull(),
                generatedAtCaptor.capture());

        assertEquals(data.generatedAt(), generatedAtCaptor.getValue());
    }

    /**
     * Creates test data for empty request using the supplied values.
     */
    private AppointmentPackGenerationRequest emptyRequest(
            UUID appointmentId) {
        return new AppointmentPackGenerationRequest(
                appointmentId,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    /**
     * Creates test data for selection using the supplied values.
     */
    private AppointmentPackSelection selection(
            Appointment appointment) {
        return new AppointmentPackSelection(
                appointment.getPatientRecord(),
                appointment,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    /**
     * Creates a test appointment with the supplied values.
     */
    private Appointment appointment(PatientRecord patientRecord, String appointmentService) {
        Appointment appointment = new Appointment();

        ReflectionTestUtils.setField(appointment, "id", UUID.randomUUID());

        appointment.setPatientRecord(patientRecord);
        appointment.setDate(LocalDate.of(2026, 9, 10));
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setService(appointmentService);

        return appointment;
    }

    /**
     * Creates a test patient record with the supplied values.
     */
    private PatientRecord patientRecord(UUID id) {
        PatientRecord patientRecord = new PatientRecord();

        ReflectionTestUtils.setField(patientRecord, "id", id);

        return patientRecord;
    }

    /**
     * Creates test data for render model using the supplied values.
     */
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
}