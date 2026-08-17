package net.imaginethinking.appointmentpack.appointment;

import net.imaginethinking.appointmentpack.address.PartialAddressRequest;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRecordAccessService patientRecordAccessService;

    @Mock
    private AppEventPublisher appEventPublisher;

    private AppointmentService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentRepository, patientRecordAccessService, appEventPublisher);
    }

    @Test
    void shouldCreateAppointmentAndNormaliseOptionalText() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();
        PatientRecord patientRecord = patientRecord(patientRecordId);

        when(patientRecordAccessService.requireAccess(userId, patientRecordId, AppointmentPermission.EDIT)).thenReturn(
                patientRecord);

        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);

            ReflectionTestUtils.setField(appointment, "id", UUID.randomUUID());

            return appointment;
        });

        AppointmentResponse response = service.createAppointment(
                userId, patientRecordId, new CreateAppointmentRequest(
                        LocalDate.of(2026, 9, 1),
                        LocalTime.of(10, 0),
                        LocalTime.of(10, 30),
                        " Neurology ",
                        " Follow-up ",
                        " Dr Smith ",
                        " Clinic A ",
                        new PartialAddressRequest(
                                " 1 Hospital Road ",
                                null,
                                " Glasgow ",
                                null,
                                " G1 1AA ",
                                " Scotland "),
                        " Bring medication list "));

        assertNotNull(response.id());
        assertEquals("Neurology", response.service());
        assertEquals("Follow-up", response.appointmentType());
        assertEquals("Dr Smith", response.clinicianOrTeam());
        assertEquals("Clinic A", response.locationName());
        assertEquals("1 Hospital Road", response.address().addressLine1());
        assertEquals("Bring medication list", response.notes());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldRejectAppointmentWhenEndTimeIsNotAfterStartTime() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        when(patientRecordAccessService.requireAccess(userId, patientRecordId, AppointmentPermission.EDIT)).thenReturn(
                patientRecord(patientRecordId));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.createAppointment(
                        userId, patientRecordId, new CreateAppointmentRequest(
                                LocalDate.of(2026, 9, 1),
                                LocalTime.of(10, 0),
                                LocalTime.of(10, 0),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void shouldUpdateExistingAppointment() {
        UUID userId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = appointment(appointmentId);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        AppointmentResponse response = service.updateAppointment(
                userId, appointmentId, new UpdateAppointmentRequest(
                        LocalDate.of(2026, 10, 2),
                        LocalTime.of(14, 0),
                        LocalTime.of(14, 45),
                        " Cardiology ",
                        null,
                        null,
                        null,
                        null,
                        " Updated notes "));

        assertEquals(LocalDate.of(2026, 10, 2), response.date());

        assertEquals(LocalTime.of(14, 0), response.startTime());

        assertEquals("Cardiology", response.service());

        assertEquals("Updated notes", response.notes());

        verify(patientRecordAccessService).requireAccess(
                userId,
                appointment.getPatientRecord(),
                AppointmentPermission.EDIT);

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldArchiveAppointmentIdempotently() {
        UUID userId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = appointment(appointmentId);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        AppointmentResponse first = service.archiveAppointment(userId, appointmentId);

        AppointmentResponse second = service.archiveAppointment(userId, appointmentId);

        assertNotNull(first.archivedAt());
        assertEquals(first.archivedAt(), second.archivedAt());

        verify(appEventPublisher, times(1)).publish(any());
    }

    @Test
    void shouldHideArchivedAppointmentFromNormalRetrieval() {
        UUID userId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = appointment(appointmentId);

        appointment.archive();

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getAppointment(userId, appointmentId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(patientRecordAccessService, never()).requireAccess(any(), any(PatientRecord.class), any());
    }

    private Appointment appointment(UUID id) {
        Appointment appointment = new Appointment();

        ReflectionTestUtils.setField(appointment, "id", id);

        appointment.setPatientRecord(patientRecord(UUID.randomUUID()));

        appointment.setDate(LocalDate.of(2026, 8, 20));

        appointment.setStartTime(LocalTime.of(9, 0));

        return appointment;
    }

    private PatientRecord patientRecord(UUID id) {
        PatientRecord patientRecord = new PatientRecord();

        ReflectionTestUtils.setField(patientRecord, "id", id);

        return patientRecord;
    }
}