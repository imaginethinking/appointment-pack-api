package net.imaginethinking.appointmentpack.medication;

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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicationServiceTest {

    @Mock
    private MedicationRepository medicationRepository;

    @Mock
    private PatientRecordAccessService patientRecordAccessService;

    @Mock
    private AppEventPublisher appEventPublisher;

    private MedicationService service;

    @BeforeEach
    void setUp() {
        service = new MedicationService(medicationRepository, patientRecordAccessService, appEventPublisher);
    }

    @Test
    void shouldCreateMedicationAndNormaliseText() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();
        PatientRecord patientRecord = patientRecord(patientRecordId);

        when(patientRecordAccessService.requireAccess(userId, patientRecordId, MedicationPermission.EDIT)).thenReturn(
                patientRecord);

        when(medicationRepository.save(any(Medication.class))).thenAnswer(invocation -> {
            Medication medication = invocation.getArgument(0);

            ReflectionTestUtils.setField(medication, "id", UUID.randomUUID());

            return medication;
        });

        MedicationResponse response = service.createMedication(
                userId, patientRecordId, new CreateMedicationRequest(
                        " Amitriptyline ",
                        " 10 mg ",
                        " Tablet ",
                        " Take at night ",
                        LocalDate.of(2026, 1, 1),
                        null,
                        "   "));

        assertEquals("Amitriptyline", response.name());

        assertEquals("10 mg", response.dose());

        assertEquals("Tablet", response.form());

        assertEquals("Take at night", response.instructions());

        assertEquals(null, response.notes());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldRejectMedicationWhenEndDateIsBeforeStartDate() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        when(patientRecordAccessService.requireAccess(userId, patientRecordId, MedicationPermission.EDIT)).thenReturn(
                patientRecord(patientRecordId));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.createMedication(
                        userId, patientRecordId, new CreateMedicationRequest(
                                "Medication",
                                null,
                                null,
                                null,
                                LocalDate.of(2026, 6, 10),
                                LocalDate.of(2026, 6, 9),
                                null)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(medicationRepository, never()).save(any());
    }

    @Test
    void shouldUpdateMedication() {
        UUID userId = UUID.randomUUID();
        UUID medicationId = UUID.randomUUID();

        Medication medication = medication(medicationId);

        when(medicationRepository.findById(medicationId)).thenReturn(Optional.of(medication));

        MedicationResponse response = service.updateMedication(
                userId, medicationId, new UpdateMedicationRequest(
                        " Updated medication ",
                        " 20 mg ",
                        null,
                        null,
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 3, 1),
                        null));

        assertEquals("Updated medication", response.name());

        assertEquals("20 mg", response.dose());

        assertEquals(LocalDate.of(2026, 3, 1), response.endDate());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldArchiveMedicationIdempotently() {
        UUID userId = UUID.randomUUID();
        UUID medicationId = UUID.randomUUID();

        Medication medication = medication(medicationId);

        when(medicationRepository.findById(medicationId)).thenReturn(Optional.of(medication));

        MedicationResponse first = service.archiveMedication(userId, medicationId);

        MedicationResponse second = service.archiveMedication(userId, medicationId);

        assertNotNull(first.archivedAt());

        assertEquals(first.archivedAt(), second.archivedAt());

        verify(appEventPublisher, times(1)).publish(any());
    }

    @Test
    void shouldHideArchivedMedicationFromNormalRetrieval() {
        UUID userId = UUID.randomUUID();
        UUID medicationId = UUID.randomUUID();

        Medication medication = medication(medicationId);

        medication.archive();

        when(medicationRepository.findById(medicationId)).thenReturn(Optional.of(medication));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getMedication(userId, medicationId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private Medication medication(UUID id) {
        Medication medication = new Medication();

        ReflectionTestUtils.setField(medication, "id", id);

        medication.setPatientRecord(patientRecord(UUID.randomUUID()));

        medication.setName("Medication");

        return medication;
    }

    private PatientRecord patientRecord(UUID id) {
        PatientRecord patientRecord = new PatientRecord();

        ReflectionTestUtils.setField(patientRecord, "id", id);

        return patientRecord;
    }
}