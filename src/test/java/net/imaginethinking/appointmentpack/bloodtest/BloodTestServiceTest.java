package net.imaginethinking.appointmentpack.bloodtest;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Checks blood test service behaviour across normal and failure cases.
 */
@ExtendWith(MockitoExtension.class)
class BloodTestServiceTest {

    @Mock
    private BloodTestRepository bloodTestRepository;

    @Mock
    private PatientRecordAccessService patientRecordAccessService;

    @Mock
    private AppEventPublisher appEventPublisher;

    private BloodTestService service;

    /**
     * Creates the common fixtures and mocks used by each test.
     */
    @BeforeEach
    void setUp() {
        service = new BloodTestService(bloodTestRepository, patientRecordAccessService, appEventPublisher);
    }

    @Test
    void shouldCreateBloodTestAndDeriveAnalyteKeyAndNumericValue() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        when(patientRecordAccessService.requireAccess(userId, patientRecordId, BloodTestPermission.EDIT)).thenReturn(
                patientRecord(patientRecordId));

        when(bloodTestRepository.save(any(BloodTest.class))).thenAnswer(invocation -> {
            BloodTest bloodTest = invocation.getArgument(0);

            ReflectionTestUtils.setField(bloodTest, "id", UUID.randomUUID());

            return bloodTest;
        });

        BloodTestResponse response = service.createBloodTest(
                userId, patientRecordId, new CreateBloodTestRequest(
                        " Full Blood Count ", LocalDate.of(2026, 8, 1), " NHS Lab ", " Routine test ", List.of(
                        new BloodTestResultRequest(
                                " Haemoglobin ",
                                " 142.5 ",
                                " g/L ",
                                " 130-170 ",
                                BloodTestResultFlag.NORMAL),
                        new BloodTestResultRequest("Comment", "Normal morphology", null, null, null))));

        assertEquals("Full Blood Count", response.title());

        assertEquals("NHS Lab", response.provider());

        assertEquals(2, response.results().size());

        assertEquals("haemoglobin", response.results().get(0).analyteKey());

        assertEquals(new BigDecimal("142.5"), response.results().get(0).numericValue());

        assertNull(response.results().get(1).numericValue());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldRejectDuplicateAnalytesAfterNormalisation() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        when(patientRecordAccessService.requireAccess(userId, patientRecordId, BloodTestPermission.EDIT)).thenReturn(
                patientRecord(patientRecordId));

        CreateBloodTestRequest request = new CreateBloodTestRequest(
                null, LocalDate.of(2026, 8, 1), null, null, List.of(
                new BloodTestResultRequest("C-Reactive Protein", "2", "mg/L", null, null),
                new BloodTestResultRequest("c reactive protein", "3", "mg/L", null, null)));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createBloodTest(userId, patientRecordId, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(bloodTestRepository, never()).save(any());
    }

    @Test
    void shouldRejectAnalyteNameWithoutLettersOrNumbers() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        when(patientRecordAccessService.requireAccess(userId, patientRecordId, BloodTestPermission.EDIT)).thenReturn(
                patientRecord(patientRecordId));

        CreateBloodTestRequest request = new CreateBloodTestRequest(
                null,
                LocalDate.of(2026, 8, 1),
                null,
                null,
                List.of(new BloodTestResultRequest("!!!", "1", null, null, null)));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createBloodTest(userId, patientRecordId, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldReplaceResultsWhenUpdatingBloodTest() {
        UUID userId = UUID.randomUUID();
        UUID bloodTestId = UUID.randomUUID();

        BloodTest bloodTest = bloodTest(bloodTestId);

        BloodTestResult existing = new BloodTestResult();

        existing.setBloodTest(bloodTest);
        existing.setAnalyteName("Old result");
        existing.setAnalyteKey("old-result");
        existing.setResultValue("1");

        bloodTest.getResults().add(existing);

        when(bloodTestRepository.findById(bloodTestId)).thenReturn(Optional.of(bloodTest));

        BloodTestResponse response = service.updateBloodTest(
                userId, bloodTestId, new UpdateBloodTestRequest(
                        "Updated",
                        LocalDate.of(2026, 8, 2),
                        null,
                        null,
                        List.of(new BloodTestResultRequest(
                                "Sodium",
                                "140",
                                "mmol/L",
                                null,
                                BloodTestResultFlag.NORMAL))));

        assertEquals(1, response.results().size());

        assertEquals("Sodium", response.results().getFirst().analyteName());

        assertEquals("sodium", response.results().getFirst().analyteKey());

        assertEquals(new BigDecimal("140"), response.results().getFirst().numericValue());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldArchiveBloodTestIdempotently() {
        UUID userId = UUID.randomUUID();
        UUID bloodTestId = UUID.randomUUID();

        BloodTest bloodTest = bloodTest(bloodTestId);

        when(bloodTestRepository.findById(bloodTestId)).thenReturn(Optional.of(bloodTest));

        BloodTestResponse first = service.archiveBloodTest(userId, bloodTestId);

        BloodTestResponse second = service.archiveBloodTest(userId, bloodTestId);

        assertNotNull(first.archivedAt());

        assertEquals(first.archivedAt(), second.archivedAt());

        verify(appEventPublisher, times(1)).publish(any());
    }

    /**
     * Creates a test blood with the supplied values.
     */
    private BloodTest bloodTest(UUID id) {
        BloodTest bloodTest = new BloodTest();

        ReflectionTestUtils.setField(bloodTest, "id", id);

        bloodTest.setPatientRecord(patientRecord(UUID.randomUUID()));

        bloodTest.setTestDate(LocalDate.of(2026, 8, 1));

        return bloodTest;
    }

    /**
     * Creates a test patient record with the supplied values.
     */
    private PatientRecord patientRecord(UUID id) {
        PatientRecord patientRecord = new PatientRecord();

        ReflectionTestUtils.setField(patientRecord, "id", id);

        return patientRecord;
    }
}