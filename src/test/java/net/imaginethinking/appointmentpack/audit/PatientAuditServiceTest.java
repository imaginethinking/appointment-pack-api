package net.imaginethinking.appointmentpack.audit;

import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import net.imaginethinking.appointmentpack.profile.Profile;
import net.imaginethinking.appointmentpack.user.User;
import net.imaginethinking.appointmentpack.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks patient audit service behaviour across normal and failure cases.
 */
@ExtendWith(MockitoExtension.class)
class PatientAuditServiceTest {

    @Mock
    private PatientAuditEventRepository patientAuditEventRepository;

    @Mock
    private PatientRecordAccessService patientRecordAccessService;

    @Mock
    private UserRepository userRepository;

    private PatientAuditService service;

    /**
     * Creates the common fixtures and mocks used by each test.
     */
    @BeforeEach
    void setUp() {
        service = new PatientAuditService(patientAuditEventRepository, patientRecordAccessService, userRepository);
    }

    @Test
    void shouldReturnAuditEventsWithActorDisplayNamesAndPagination() {
        UUID authenticatedUserId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        PatientAuditEvent event = auditEvent(
                patientRecordId,
                actorUserId,
                PatientResourceType.APPOINTMENT,
                PatientActivityAction.UPDATED);

        PageRequest pageRequest = PageRequest.of(0, 50);

        when(patientAuditEventRepository.findAllByPatientRecordIdOrderByOccurredAtDesc(
                patientRecordId,
                pageRequest)).thenReturn(new PageImpl<>(List.of(event), pageRequest, 1));

        when(userRepository.findAllByIdIn(Set.of(actorUserId))).thenReturn(List.of(user(actorUserId, "Jane", "Carer")));

        PatientAuditPageResponse response = service.getAuditEvents(authenticatedUserId, patientRecordId, 0, 50);

        verify(patientRecordAccessService).requireAccess(authenticatedUserId, patientRecordId, AuditPermission.VIEW);

        assertEquals(1, response.events().size());
        assertEquals(0, response.page());
        assertEquals(50, response.size());
        assertEquals(1, response.totalElements());
        assertEquals(1, response.totalPages());

        PatientAuditResponse item = response.events().getFirst();

        assertEquals(actorUserId, item.actorUserId());
        assertEquals("Jane Carer", item.actorDisplayName());
        assertEquals(PatientResourceType.APPOINTMENT, item.resourceType());
        assertEquals(PatientActivityAction.UPDATED, item.action());
    }

    @Test
    void shouldHandleAuditActorThatNoLongerExists() {
        UUID authenticatedUserId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        PatientAuditEvent event = auditEvent(
                patientRecordId,
                actorUserId,
                PatientResourceType.MEDICATION,
                PatientActivityAction.ARCHIVED);

        PageRequest pageRequest = PageRequest.of(0, 20);

        when(patientAuditEventRepository.findAllByPatientRecordIdOrderByOccurredAtDesc(
                patientRecordId,
                pageRequest)).thenReturn(new PageImpl<>(List.of(event), pageRequest, 1));

        when(userRepository.findAllByIdIn(Set.of(actorUserId))).thenReturn(List.of());

        PatientAuditPageResponse response = service.getAuditEvents(authenticatedUserId, patientRecordId, 0, 20);

        assertNull(response.events().getFirst().actorDisplayName());
    }

    @Test
    void shouldAvoidUserLookupWhenAuditPageIsEmpty() {
        UUID authenticatedUserId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();
        PageRequest pageRequest = PageRequest.of(1, 25);

        when(patientAuditEventRepository.findAllByPatientRecordIdOrderByOccurredAtDesc(
                patientRecordId,
                pageRequest)).thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        PatientAuditPageResponse response = service.getAuditEvents(authenticatedUserId, patientRecordId, 1, 25);

        assertEquals(0, response.events().size());
        assertEquals(1, response.page());
        assertEquals(25, response.size());

        verify(patientRecordAccessService).requireAccess(authenticatedUserId, patientRecordId, AuditPermission.VIEW);
    }

    /**
     * Creates test data for audit event using the supplied values.
     */
    private PatientAuditEvent auditEvent(
            UUID patientRecordId,
            UUID actorUserId,
            PatientResourceType resourceType,
            PatientActivityAction action) {
        return new PatientAuditEvent(
                UUID.randomUUID(),
                patientRecordId,
                actorUserId,
                resourceType,
                UUID.randomUUID(),
                action,
                Instant.parse("2026-08-14T04:00:00Z"));
    }

    /**
     * Creates a test user with the supplied values.
     */
    private User user(UUID userId, String firstName, String lastName) {
        User user = new User();

        ReflectionTestUtils.setField(user, "id", userId);

        Profile profile = new Profile();
        profile.setUser(user);
        profile.setFirstName(firstName);
        profile.setLastName(lastName);

        user.setProfile(profile);

        return user;
    }
}