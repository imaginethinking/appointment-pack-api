package net.imaginethinking.appointmentpack.pack.generation;

import net.imaginethinking.appointmentpack.appointment.Appointment;
import net.imaginethinking.appointmentpack.appointment.AppointmentPermission;
import net.imaginethinking.appointmentpack.appointment.AppointmentRepository;
import net.imaginethinking.appointmentpack.bloodtest.BloodTest;
import net.imaginethinking.appointmentpack.bloodtest.BloodTestRepository;
import net.imaginethinking.appointmentpack.contact.ContactPermission;
import net.imaginethinking.appointmentpack.contact.EmergencyContactRepository;
import net.imaginethinking.appointmentpack.contact.HealthcareContactRepository;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryEntryRepository;
import net.imaginethinking.appointmentpack.medication.Medication;
import net.imaginethinking.appointmentpack.medication.MedicationPermission;
import net.imaginethinking.appointmentpack.medication.MedicationRepository;
import net.imaginethinking.appointmentpack.pack.AppointmentPackGenerationRequest;
import net.imaginethinking.appointmentpack.pack.AppointmentPackItemType;
import net.imaginethinking.appointmentpack.pack.AppointmentPackPermission;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentPackSelectionServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private MedicationRepository medicationRepository;

    @Mock
    private HealthcareContactRepository healthcareContactRepository;

    @Mock
    private EmergencyContactRepository emergencyContactRepository;

    @Mock
    private MedicalHistoryEntryRepository medicalHistoryEntryRepository;

    @Mock
    private BloodTestRepository bloodTestRepository;

    @Mock
    private PatientRecordAccessService patientRecordAccessService;

    private AppointmentPackSelectionService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentPackSelectionService(
                appointmentRepository,
                medicationRepository,
                healthcareContactRepository,
                emergencyContactRepository,
                medicalHistoryEntryRepository,
                bloodTestRepository,
                patientRecordAccessService);
    }

    @Test
    void shouldResolveSelectedResourcesInRequestedOrder() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();
        PatientRecord patientRecord = patientRecord(patientRecordId);
        Appointment appointment = appointment(patientRecord);

        Medication firstMedication = medication(patientRecord);
        Medication secondMedication = medication(patientRecord);
        BloodTest bloodTest = bloodTest(patientRecord);

        AppointmentPackGenerationRequest request = request(
                appointment.getId(),
                List.of(secondMedication.getId(), firstMedication.getId()),
                List.of(),
                List.of(),
                List.of(),
                List.of(bloodTest.getId()));

        when(patientRecordAccessService.requireAccess(
                userId,
                patientRecordId,
                AppointmentPackPermission.CREATE)).thenReturn(patientRecord);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        when(medicationRepository.findAllById(request.medicationIds())).thenReturn(List.of(
                firstMedication,
                secondMedication));

        when(bloodTestRepository.findAllById(request.bloodTestIds())).thenReturn(List.of(bloodTest));

        AppointmentPackSelection selection = service.select(userId, patientRecordId, request);

        assertEquals(
                List.of(secondMedication.getId(), firstMedication.getId()),
                selection.medications().stream().map(Medication::getId).toList());

        assertEquals(
                List.of(
                        AppointmentPackItemType.MEDICATION,
                        AppointmentPackItemType.MEDICATION,
                        AppointmentPackItemType.BLOOD_TEST),
                selection.selectedItems()
                        .stream()
                        .map(AppointmentPackGenerationData.SelectedItem::resourceType)
                        .toList());

        assertEquals(
                List.of(0, 1, 2),
                selection.selectedItems()
                        .stream()
                        .map(AppointmentPackGenerationData.SelectedItem::displayOrder)
                        .toList());

        verify(patientRecordAccessService).requireAccess(userId, patientRecord, AppointmentPermission.VIEW);

        verify(patientRecordAccessService).requireAccess(userId, patientRecord, MedicationPermission.VIEW);

        verify(patientRecordAccessService).requireAccess(
                userId,
                patientRecord,
                net.imaginethinking.appointmentpack.bloodtest.BloodTestPermission.VIEW);

        verify(patientRecordAccessService, never()).requireAccess(userId, patientRecord, ContactPermission.VIEW);
    }

    @Test
    void shouldRejectDuplicateSelectedResourceIds() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();
        PatientRecord patientRecord = patientRecord(patientRecordId);
        Appointment appointment = appointment(patientRecord);
        UUID medicationId = UUID.randomUUID();

        AppointmentPackGenerationRequest request = request(
                appointment.getId(),
                List.of(medicationId, medicationId),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        when(patientRecordAccessService.requireAccess(
                userId,
                patientRecordId,
                AppointmentPackPermission.CREATE)).thenReturn(patientRecord);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.select(userId, patientRecordId, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(medicationRepository, never()).findAllById(any());
    }

    @Test
    void shouldRejectSelectedResourceBelongingToAnotherPatient() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();
        PatientRecord patientRecord = patientRecord(patientRecordId);
        PatientRecord otherPatientRecord = patientRecord(UUID.randomUUID());
        Appointment appointment = appointment(patientRecord);
        Medication medication = medication(otherPatientRecord);

        AppointmentPackGenerationRequest request = request(
                appointment.getId(),
                List.of(medication.getId()),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        when(patientRecordAccessService.requireAccess(
                userId,
                patientRecordId,
                AppointmentPackPermission.CREATE)).thenReturn(patientRecord);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        when(medicationRepository.findAllById(request.medicationIds())).thenReturn(List.of(medication));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.select(userId, patientRecordId, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldRejectArchivedSelectedResource() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();
        PatientRecord patientRecord = patientRecord(patientRecordId);
        Appointment appointment = appointment(patientRecord);
        Medication medication = medication(patientRecord);

        medication.archive();

        AppointmentPackGenerationRequest request = request(
                appointment.getId(),
                List.of(medication.getId()),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        when(patientRecordAccessService.requireAccess(
                userId,
                patientRecordId,
                AppointmentPackPermission.CREATE)).thenReturn(patientRecord);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        when(medicationRepository.findAllById(request.medicationIds())).thenReturn(List.of(medication));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.select(userId, patientRecordId, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldRejectAppointmentBelongingToAnotherPatient() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();
        PatientRecord patientRecord = patientRecord(patientRecordId);

        Appointment appointment = appointment(patientRecord(UUID.randomUUID()));

        AppointmentPackGenerationRequest request = request(
                appointment.getId(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        when(patientRecordAccessService.requireAccess(
                userId,
                patientRecordId,
                AppointmentPackPermission.CREATE)).thenReturn(patientRecord);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.select(userId, patientRecordId, request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private AppointmentPackGenerationRequest request(
            UUID appointmentId,
            List<UUID> medicationIds,
            List<UUID> healthcareContactIds,
            List<UUID> emergencyContactIds,
            List<UUID> medicalHistoryEntryIds,
            List<UUID> bloodTestIds) {
        return new AppointmentPackGenerationRequest(
                appointmentId,
                null,
                null,
                medicationIds,
                healthcareContactIds,
                emergencyContactIds,
                medicalHistoryEntryIds,
                bloodTestIds);
    }

    private Appointment appointment(
            PatientRecord patientRecord) {
        Appointment appointment = new Appointment();

        ReflectionTestUtils.setField(appointment, "id", UUID.randomUUID());

        appointment.setPatientRecord(patientRecord);
        appointment.setDate(LocalDate.of(2026, 9, 10));
        appointment.setStartTime(LocalTime.of(10, 0));

        return appointment;
    }

    private Medication medication(
            PatientRecord patientRecord) {
        Medication medication = new Medication();

        ReflectionTestUtils.setField(medication, "id", UUID.randomUUID());

        medication.setPatientRecord(patientRecord);
        medication.setName("Medication");

        return medication;
    }

    private BloodTest bloodTest(
            PatientRecord patientRecord) {
        BloodTest bloodTest = new BloodTest();

        ReflectionTestUtils.setField(bloodTest, "id", UUID.randomUUID());

        bloodTest.setPatientRecord(patientRecord);
        bloodTest.setTestDate(LocalDate.of(2026, 8, 1));

        return bloodTest;
    }

    private PatientRecord patientRecord(UUID id) {
        PatientRecord patientRecord = new PatientRecord();

        ReflectionTestUtils.setField(patientRecord, "id", id);

        return patientRecord;
    }
}