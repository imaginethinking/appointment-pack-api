package net.imaginethinking.appointmentpack.patientrecord;

import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.profile.Profile;
import net.imaginethinking.appointmentpack.profile.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Checks patient record service behaviour across normal and failure cases.
 */
@ExtendWith(MockitoExtension.class)
class PatientRecordServiceTest {

    @Mock
    private PatientRecordRepository patientRecordRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private PatientRecordAccessService patientRecordAccessService;

    @Mock
    private AppEventPublisher appEventPublisher;

    private PatientRecordService service;

    /**
     * Creates the common fixtures and mocks used by each test.
     */
    @BeforeEach
    void setUp() {
        service = new PatientRecordService(
                patientRecordRepository,
                profileRepository,
                patientRecordAccessService,
                appEventPublisher);
    }

    @Test
    void shouldCreatePatientRecordAndCalculateBmi() {
        UUID userId = UUID.randomUUID();

        Profile profile = new Profile();

        ReflectionTestUtils.setField(profile, "id", UUID.randomUUID());

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(patientRecordRepository.existsByProfileId(profile.getId())).thenReturn(false);

        when(patientRecordRepository.save(any(PatientRecord.class))).thenAnswer(invocation -> {
            PatientRecord record = invocation.getArgument(0);

            ReflectionTestUtils.setField(record, "id", UUID.randomUUID());

            return record;
        });

        PatientRecordResponse response = service.createCurrentPatientRecord(
                userId, new CreatePatientRecordRequest(
                        " 1234567890 ",
                        null,
                        null,
                        new BigDecimal("180"),
                        HeightUnit.CENTIMETERS,
                        new BigDecimal("81"),
                        WeightUnit.KILOGRAMS,
                        BloodType.O_POSITIVE));

        assertEquals("1234567890", response.nhsNumber());
        assertEquals(new BigDecimal("25.00"), response.bmi());
        assertEquals(BloodType.O_POSITIVE, response.bloodType());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldCreatePatientRecordWithoutOptionalMeasurements() {
        UUID userId = UUID.randomUUID();

        Profile profile = new Profile();

        ReflectionTestUtils.setField(profile, "id", UUID.randomUUID());

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(patientRecordRepository.existsByProfileId(profile.getId())).thenReturn(false);

        when(patientRecordRepository.save(any(PatientRecord.class))).thenAnswer(invocation -> {
            PatientRecord record = invocation.getArgument(0);

            ReflectionTestUtils.setField(record, "id", UUID.randomUUID());

            return record;
        });

        PatientRecordResponse response = service.createCurrentPatientRecord(userId, emptyCreateRequest());

        assertNull(response.height());
        assertNull(response.heightUnit());
        assertNull(response.weight());
        assertNull(response.weightUnit());
        assertNull(response.bmi());
    }

    @Test
    void shouldRejectDuplicatePatientRecord() {
        UUID userId = UUID.randomUUID();

        Profile profile = new Profile();

        ReflectionTestUtils.setField(profile, "id", UUID.randomUUID());

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(patientRecordRepository.existsByProfileId(profile.getId())).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createCurrentPatientRecord(userId, emptyCreateRequest()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(patientRecordRepository, never()).save(any());
    }

    @Test
    void shouldRejectHeightValueWithoutUnit() {
        UUID userId = UUID.randomUUID();
        Profile profile = availableProfile(userId);

        CreatePatientRecordRequest request = new CreatePatientRecordRequest(
                null,
                null,
                null,
                new BigDecimal("180"),
                null,
                null,
                null,
                null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createCurrentPatientRecord(userId, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(patientRecordRepository, never()).save(any());
    }

    @Test
    void shouldRejectHeightUnitWithoutValue() {
        UUID userId = UUID.randomUUID();
        Profile profile = availableProfile(userId);

        CreatePatientRecordRequest request = new CreatePatientRecordRequest(
                null,
                null,
                null,
                null,
                HeightUnit.CENTIMETERS,
                null,
                null,
                null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createCurrentPatientRecord(userId, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(patientRecordRepository, never()).save(any());
    }

    @Test
    void shouldRejectWeightValueWithoutUnit() {
        UUID userId = UUID.randomUUID();
        Profile profile = availableProfile(userId);

        CreatePatientRecordRequest request = new CreatePatientRecordRequest(
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("75"),
                null,
                null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createCurrentPatientRecord(userId, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(patientRecordRepository, never()).save(any());
    }

    @Test
    void shouldRejectWeightUnitWithoutValue() {
        UUID userId = UUID.randomUUID();
        Profile profile = availableProfile(userId);

        CreatePatientRecordRequest request = new CreatePatientRecordRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                WeightUnit.KILOGRAMS,
                null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createCurrentPatientRecord(userId, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(patientRecordRepository, never()).save(any());
    }

    @Test
    void shouldUpdatePatientRecordThroughAccessService() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        PatientRecord record = record(patientRecordId);

        when(patientRecordAccessService.requireAccess(
                userId,
                patientRecordId,
                PatientRecordPermission.EDIT)).thenReturn(record);

        PatientRecordResponse response = service.updatePatientRecord(
                userId,
                patientRecordId,
                new UpdatePatientRecordRequest("9999999999", null, null, null, null, null, null, BloodType.A_POSITIVE));

        assertEquals("9999999999", response.nhsNumber());
        assertEquals(BloodType.A_POSITIVE, response.bloodType());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldRequireViewPermissionForSpecificPatientRecord() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        PatientRecord record = record(patientRecordId);

        when(patientRecordAccessService.requireAccess(
                userId,
                patientRecordId,
                PatientRecordPermission.VIEW)).thenReturn(record);

        PatientRecordResponse response = service.getPatientRecord(userId, patientRecordId);

        assertEquals(patientRecordId, response.id());

        verify(patientRecordAccessService).requireAccess(userId, patientRecordId, PatientRecordPermission.VIEW);
    }

    /**
     * Sets up the mocked available profile behaviour used by the current test.
     */
    private Profile availableProfile(UUID userId) {
        Profile profile = new Profile();

        ReflectionTestUtils.setField(profile, "id", UUID.randomUUID());

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(patientRecordRepository.existsByProfileId(profile.getId())).thenReturn(false);

        return profile;
    }

    /**
     * Creates test data for empty create request using the supplied values.
     */
    private CreatePatientRecordRequest emptyCreateRequest() {
        return new CreatePatientRecordRequest(null, null, null, null, null, null, null, null);
    }

    /**
     * Creates a test record with the supplied values.
     */
    private PatientRecord record(UUID id) {
        Profile profile = new Profile();

        ReflectionTestUtils.setField(profile, "id", UUID.randomUUID());

        PatientRecord record = new PatientRecord();

        ReflectionTestUtils.setField(record, "id", id);

        record.setProfile(profile);

        return record;
    }
}