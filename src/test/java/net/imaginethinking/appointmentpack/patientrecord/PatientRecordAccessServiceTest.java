package net.imaginethinking.appointmentpack.patientrecord;

import net.imaginethinking.appointmentpack.patientcareraccess.PatientAccessControlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientRecordAccessServiceTest {

    @Mock
    private PatientRecordRepository patientRecordRepository;

    @Mock
    private PatientAccessControlService patientAccessControlService;

    private PatientRecordAccessService service;

    @BeforeEach
    void setUp() {
        service = new PatientRecordAccessService(patientRecordRepository, patientAccessControlService);
    }

    @Test
    void shouldReturnExistingPatientRecord() {
        UUID patientRecordId = UUID.randomUUID();
        PatientRecord record = new PatientRecord();

        when(patientRecordRepository.findById(patientRecordId)).thenReturn(Optional.of(record));

        assertSame(record, service.requirePatientRecord(patientRecordId));
    }

    @Test
    void shouldReturnNotFoundForMissingPatientRecord() {
        UUID patientRecordId = UUID.randomUUID();

        when(patientRecordRepository.findById(patientRecordId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.requirePatientRecord(patientRecordId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldDelegatePermissionCheckAndReturnRecord() {
        UUID userId = UUID.randomUUID();
        PatientRecord record = new PatientRecord();

        PatientRecord result = service.requireAccess(userId, record, PatientRecordPermission.VIEW);

        assertSame(record, result);

        verify(patientAccessControlService).requirePermission(userId, record, PatientRecordPermission.VIEW);
    }
}