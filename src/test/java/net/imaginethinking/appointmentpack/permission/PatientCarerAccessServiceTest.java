package net.imaginethinking.appointmentpack.patientcareraccess;

import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordRepository;
import net.imaginethinking.appointmentpack.permission.PermissionValidator;
import net.imaginethinking.appointmentpack.profile.Profile;
import net.imaginethinking.appointmentpack.user.User;
import net.imaginethinking.appointmentpack.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientCarerAccessServiceTest {

    @Mock
    private PatientCarerAccessRepository patientCarerAccessRepository;

    @Mock
    private PatientRecordRepository patientRecordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionValidator permissionValidator;

    @Mock
    private AppEventPublisher appEventPublisher;

    private PatientCarerAccessService service;

    @BeforeEach
    void setUp() {
        service = new PatientCarerAccessService(
                patientCarerAccessRepository,
                patientRecordRepository,
                userRepository,
                permissionValidator,
                appEventPublisher);
    }

    @Test
    void shouldCreatePendingInvitationForRegisteredCarer() {
        User owner = user("patient@example.com", "Patient", "User");

        User carer = user("carer@example.com", "Carer", "User");

        PatientRecord record = patientRecord(owner);

        Set<String> permissions = Set.of("patient-record:view");

        when(patientRecordRepository.findByProfileUserId(owner.getId())).thenReturn(Optional.of(record));

        when(userRepository.findByEmail("carer@example.com")).thenReturn(Optional.of(carer));

        when(permissionValidator.validate(permissions)).thenReturn(permissions);

        when(patientCarerAccessRepository.findByPatientRecord_IdAndCarer_Id(record.getId(), carer.getId())).thenReturn(
                Optional.empty());

        when(patientCarerAccessRepository.save(any(PatientCarerAccess.class))).thenAnswer(invocation -> {
            PatientCarerAccess access = invocation.getArgument(0);

            ReflectionTestUtils.setField(access, "id", UUID.randomUUID());

            return access;
        });

        PatientCarerAccessResponse response = service.createInvitation(
                owner.getId(),
                new CreateCarerInvitationRequest(" Carer@Example.com ", permissions));

        assertEquals(PatientCarerAccessStatus.PENDING, response.status());
        assertEquals(permissions, response.permissions());
        assertEquals(carer.getId(), response.carer().userId());
        assertNotNull(response.invitedAt());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldRejectSelfInvitation() {
        User owner = user("patient@example.com", "Patient", "User");

        PatientRecord record = patientRecord(owner);

        when(patientRecordRepository.findByProfileUserId(owner.getId())).thenReturn(Optional.of(record));

        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(owner));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.createInvitation(
                        owner.getId(),
                        new CreateCarerInvitationRequest("patient@example.com", Set.of("patient-record:view"))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldAllowCarerToAcceptPendingInvitation() {
        User owner = user("patient@example.com", "Patient", "User");

        User carer = user("carer@example.com", "Carer", "User");

        PatientCarerAccess access = access(
                owner,
                carer,
                PatientCarerAccessStatus.PENDING,
                Set.of("patient-record:view"));

        when(patientCarerAccessRepository.findById(access.getId())).thenReturn(Optional.of(access));

        PatientCarerAccessResponse response = service.acceptInvitation(carer.getId(), access.getId());

        assertEquals(PatientCarerAccessStatus.ACTIVE, response.status());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldAllowOwnerToRevokeActiveAccess() {
        User owner = user("patient@example.com", "Patient", "User");

        User carer = user("carer@example.com", "Carer", "User");

        PatientCarerAccess access = access(
                owner,
                carer,
                PatientCarerAccessStatus.ACTIVE,
                Set.of("patient-record:view"));

        when(patientCarerAccessRepository.findById(access.getId())).thenReturn(Optional.of(access));

        PatientCarerAccessResponse response = service.revokeAccess(owner.getId(), access.getId());

        assertEquals(PatientCarerAccessStatus.REVOKED, response.status());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldRejectInvalidRelationshipStateTransition() {
        User owner = user("patient@example.com", "Patient", "User");

        User carer = user("carer@example.com", "Carer", "User");

        PatientCarerAccess access = access(
                owner,
                carer,
                PatientCarerAccessStatus.ACTIVE,
                Set.of("patient-record:view"));

        when(patientCarerAccessRepository.findById(access.getId())).thenReturn(Optional.of(access));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.acceptInvitation(carer.getId(), access.getId()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void shouldUpdatePermissionsWithoutChangingRelationshipStatusTimestamp() {
        User owner = user("patient@example.com", "Patient", "User");

        User carer = user("carer@example.com", "Carer", "User");

        PatientCarerAccess access = access(
                owner,
                carer,
                PatientCarerAccessStatus.ACTIVE,
                Set.of("patient-record:view"));

        Instant originalStatusChangedAt = access.getStatusChangedAt();

        Set<String> updatedPermissions = Set.of("patient-record:view", "patient-record:edit");

        when(patientCarerAccessRepository.findById(access.getId())).thenReturn(Optional.of(access));

        when(permissionValidator.validate(updatedPermissions)).thenReturn(updatedPermissions);

        PatientCarerAccessResponse response = service.updatePermissions(
                owner.getId(),
                access.getId(),
                new UpdatePatientCarerPermissionsRequest(updatedPermissions));

        assertEquals(updatedPermissions, response.permissions());

        assertEquals(originalStatusChangedAt, response.statusChangedAt());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldTreatIdenticalPermissionUpdateAsNoOp() {
        User owner = user("patient@example.com", "Patient", "User");

        User carer = user("carer@example.com", "Carer", "User");

        Set<String> permissions = Set.of("patient-record:view");

        PatientCarerAccess access = access(owner, carer, PatientCarerAccessStatus.ACTIVE, permissions);

        when(patientCarerAccessRepository.findById(access.getId())).thenReturn(Optional.of(access));

        when(permissionValidator.validate(permissions)).thenReturn(permissions);

        PatientCarerAccessResponse response = service.updatePermissions(
                owner.getId(),
                access.getId(),
                new UpdatePatientCarerPermissionsRequest(permissions));

        assertEquals(permissions, response.permissions());

        verify(appEventPublisher, never()).publish(any());
    }

    private PatientCarerAccess access(
            User owner,
            User carer,
            PatientCarerAccessStatus status,
            Set<String> permissions) {
        PatientCarerAccess access = new PatientCarerAccess();

        ReflectionTestUtils.setField(access, "id", UUID.randomUUID());

        access.setPatientRecord(patientRecord(owner));

        access.setCarer(carer);
        access.setStatus(status);

        access.setPermissions(new HashSet<>(permissions));

        access.setInvitedAt(Instant.now().minusSeconds(60));

        access.setStatusChangedAt(Instant.now().minusSeconds(30));

        return access;
    }

    private PatientRecord patientRecord(
            User owner) {
        PatientRecord record = new PatientRecord();

        ReflectionTestUtils.setField(record, "id", UUID.randomUUID());

        record.setProfile(owner.getProfile());

        return record;
    }

    private User user(String email, String firstName, String lastName) {
        User user = new User();

        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        user.setEmail(email);

        Profile profile = new Profile();

        ReflectionTestUtils.setField(profile, "id", UUID.randomUUID());

        profile.setUser(user);
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setDateOfBirth(LocalDate.of(1990, 1, 1));

        user.setProfile(profile);

        return user;
    }
}