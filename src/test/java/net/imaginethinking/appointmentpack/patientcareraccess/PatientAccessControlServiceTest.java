package net.imaginethinking.appointmentpack.patientcareraccess;

import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordPermission;
import net.imaginethinking.appointmentpack.profile.Profile;
import net.imaginethinking.appointmentpack.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientAccessControlServiceTest {

    @Mock
    private PatientCarerAccessRepository repository;

    private PatientAccessControlService service;

    @BeforeEach
    void setUp() {
        service = new PatientAccessControlService(repository);
    }

    @Test
    void shouldAlwaysAllowPatientOwner() {
        UUID ownerId = UUID.randomUUID();
        PatientRecord record = patientRecord(ownerId);

        assertDoesNotThrow(() -> service.requirePermission(ownerId, record, PatientRecordPermission.EDIT));

        verify(repository, never()).findByPatientRecordIdAndCarerIdAndStatus(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldAllowActiveCarerWithRequiredPermission() {
        UUID carerId = UUID.randomUUID();

        PatientRecord record = patientRecord(UUID.randomUUID());

        PatientCarerAccess access = new PatientCarerAccess();

        access.setPermissions(new HashSet<>(java.util.Set.of("patient-record:view")));

        when(repository.findByPatientRecordIdAndCarerIdAndStatus(
                record.getId(),
                carerId,
                PatientCarerAccessStatus.ACTIVE)).thenReturn(Optional.of(access));

        assertDoesNotThrow(() -> service.requirePermission(carerId, record, PatientRecordPermission.VIEW));
    }

    @Test
    void shouldRejectCarerWithoutActiveRelationship() {
        UUID carerId = UUID.randomUUID();

        PatientRecord record = patientRecord(UUID.randomUUID());

        when(repository.findByPatientRecordIdAndCarerIdAndStatus(
                record.getId(),
                carerId,
                PatientCarerAccessStatus.ACTIVE)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.requirePermission(carerId, record, PatientRecordPermission.VIEW));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void shouldRejectActiveCarerWithoutRequiredPermission() {
        UUID carerId = UUID.randomUUID();

        PatientRecord record = patientRecord(UUID.randomUUID());

        PatientCarerAccess access = new PatientCarerAccess();

        access.setPermissions(new HashSet<>());

        when(repository.findByPatientRecordIdAndCarerIdAndStatus(
                record.getId(),
                carerId,
                PatientCarerAccessStatus.ACTIVE)).thenReturn(Optional.of(access));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.requirePermission(carerId, record, PatientRecordPermission.VIEW));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    private PatientRecord patientRecord(
            UUID ownerUserId) {
        User owner = new User();

        ReflectionTestUtils.setField(owner, "id", ownerUserId);

        Profile profile = new Profile();
        profile.setUser(owner);

        PatientRecord record = new PatientRecord();

        ReflectionTestUtils.setField(record, "id", UUID.randomUUID());

        record.setProfile(profile);

        return record;
    }
}