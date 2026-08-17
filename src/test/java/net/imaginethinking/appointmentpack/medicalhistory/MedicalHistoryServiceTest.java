package net.imaginethinking.appointmentpack.medicalhistory;

import jakarta.persistence.EntityManager;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import net.imaginethinking.appointmentpack.user.User;
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
class MedicalHistoryServiceTest {

    @Mock
    private MedicalHistoryEntryRepository medicalHistoryEntryRepository;

    @Mock
    private PatientRecordAccessService patientRecordAccessService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private AppEventPublisher appEventPublisher;

    private MedicalHistoryService service;

    @BeforeEach
    void setUp() {
        service = new MedicalHistoryService(
                medicalHistoryEntryRepository,
                patientRecordAccessService,
                entityManager,
                appEventPublisher);
    }

    @Test
    void shouldCreateManualMedicalHistoryEntry() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        PatientRecord patientRecord = patientRecord(patientRecordId);

        User createdBy = user(userId);

        when(patientRecordAccessService.requireAccess(
                userId,
                patientRecordId,
                MedicalHistoryPermission.EDIT)).thenReturn(patientRecord);

        when(entityManager.getReference(User.class, userId)).thenReturn(createdBy);

        when(medicalHistoryEntryRepository.save(any(MedicalHistoryEntry.class))).thenAnswer(invocation -> {
            MedicalHistoryEntry entry = invocation.getArgument(0);

            ReflectionTestUtils.setField(entry, "id", UUID.randomUUID());

            return entry;
        });

        MedicalHistoryEntryResponse response = service.createMedicalHistoryEntry(
                userId,
                patientRecordId,
                new CreateMedicalHistoryEntryRequest(
                        " Migraine history ",
                        " Intermittent headaches over six months. ",
                        LocalDate.of(2026, 7, 1)));

        assertEquals("Migraine history", response.title());

        assertEquals("Intermittent headaches over six months.", response.summary());

        assertEquals(MedicalHistorySourceType.MANUAL, response.sourceType());

        assertNull(response.sourceDocumentId());

        assertEquals(userId, response.createdByUserId());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldUpdateMedicalHistoryEntry() {
        UUID userId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        MedicalHistoryEntry entry = entry(entryId, userId);

        when(medicalHistoryEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        MedicalHistoryEntryResponse response = service.updateMedicalHistoryEntry(
                userId,
                entryId,
                new UpdateMedicalHistoryEntryRequest(" Updated title ", " Updated summary ", LocalDate.of(2026, 7, 2)));

        assertEquals("Updated title", response.title());

        assertEquals("Updated summary", response.summary());

        assertEquals(LocalDate.of(2026, 7, 2), response.entryDate());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldArchiveMedicalHistoryEntryIdempotently() {
        UUID userId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        MedicalHistoryEntry entry = entry(entryId, userId);

        when(medicalHistoryEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        MedicalHistoryEntryResponse first = service.archiveMedicalHistoryEntry(userId, entryId);

        MedicalHistoryEntryResponse second = service.archiveMedicalHistoryEntry(userId, entryId);

        assertNotNull(first.archivedAt());

        assertEquals(first.archivedAt(), second.archivedAt());

        verify(appEventPublisher, times(1)).publish(any());
    }

    @Test
    void shouldHideArchivedMedicalHistoryEntryFromNormalRetrieval() {
        UUID userId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        MedicalHistoryEntry entry = entry(entryId, userId);

        entry.archive();

        when(medicalHistoryEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getMedicalHistoryEntry(userId, entryId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundForMissingMedicalHistoryEntry() {
        UUID userId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        when(medicalHistoryEntryRepository.findById(entryId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getMedicalHistoryEntry(userId, entryId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private MedicalHistoryEntry entry(UUID id, UUID userId) {
        MedicalHistoryEntry entry = new MedicalHistoryEntry();

        ReflectionTestUtils.setField(entry, "id", id);

        entry.setPatientRecord(patientRecord(UUID.randomUUID()));

        entry.setTitle("History");
        entry.setSummary("Summary");

        entry.setEntryDate(LocalDate.of(2026, 7, 1));

        entry.setSourceType(MedicalHistorySourceType.MANUAL);

        entry.setCreatedBy(user(userId));

        return entry;
    }

    private User user(UUID id) {
        User user = new User();

        ReflectionTestUtils.setField(user, "id", id);

        return user;
    }

    private PatientRecord patientRecord(UUID id) {
        PatientRecord patientRecord = new PatientRecord();

        ReflectionTestUtils.setField(patientRecord, "id", id);

        return patientRecord;
    }
}