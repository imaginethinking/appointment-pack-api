package net.imaginethinking.appointmentpack.contact;

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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyContactServiceTest {

    @Mock
    private EmergencyContactRepository emergencyContactRepository;

    @Mock
    private PatientRecordAccessService patientRecordAccessService;

    @Mock
    private AppEventPublisher appEventPublisher;

    private EmergencyContactService service;

    @BeforeEach
    void setUp() {
        service = new EmergencyContactService(
                emergencyContactRepository,
                patientRecordAccessService,
                appEventPublisher);
    }

    @Test
    void shouldCreateEmergencyContactAndNormaliseValues() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        when(patientRecordAccessService.requireAccess(userId, patientRecordId, ContactPermission.EDIT)).thenReturn(
                patientRecord(patientRecordId));

        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenAnswer(invocation -> {
            EmergencyContact contact = invocation.getArgument(0);

            ReflectionTestUtils.setField(contact, "id", UUID.randomUUID());

            return contact;
        });

        EmergencyContactResponse response = service.createEmergencyContact(
                userId,
                patientRecordId,
                new CreateEmergencyContactRequest(
                        " Jane Doe ",
                        " Daughter ",
                        " 07123 456789 ",
                        "   ",
                        " jane@example.com ",
                        " Call first "));

        assertEquals("Jane Doe", response.name());

        assertEquals("Daughter", response.relationship());

        assertEquals("07123 456789", response.phoneNumber());

        assertEquals(null, response.alternativePhoneNumber());

        assertEquals("jane@example.com", response.email());

        assertEquals("Call first", response.notes());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldUpdateEmergencyContact() {
        UUID userId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();

        EmergencyContact contact = emergencyContact(contactId);

        when(emergencyContactRepository.findById(contactId)).thenReturn(Optional.of(contact));

        EmergencyContactResponse response = service.updateEmergencyContact(
                userId,
                contactId,
                new UpdateEmergencyContactRequest(" John Doe ", " Brother ", " 07000 000000 ", null, null, null));

        assertEquals("John Doe", response.name());

        assertEquals("Brother", response.relationship());

        assertEquals("07000 000000", response.phoneNumber());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldArchiveEmergencyContactIdempotently() {
        UUID userId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();

        EmergencyContact contact = emergencyContact(contactId);

        when(emergencyContactRepository.findById(contactId)).thenReturn(Optional.of(contact));

        EmergencyContactResponse first = service.archiveEmergencyContact(userId, contactId);

        EmergencyContactResponse second = service.archiveEmergencyContact(userId, contactId);

        assertNotNull(first.archivedAt());

        assertEquals(first.archivedAt(), second.archivedAt());

        verify(appEventPublisher, times(1)).publish(any());
    }

    @Test
    void shouldHideArchivedEmergencyContactFromNormalRetrieval() {
        UUID userId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();

        EmergencyContact contact = emergencyContact(contactId);

        contact.archive();

        when(emergencyContactRepository.findById(contactId)).thenReturn(Optional.of(contact));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getEmergencyContact(userId, contactId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private EmergencyContact emergencyContact(
            UUID id) {
        EmergencyContact contact = new EmergencyContact();

        ReflectionTestUtils.setField(
                contact, "id", id);

        contact.setPatientRecord(patientRecord(UUID.randomUUID()));

        contact.setName("Contact");
        contact.setRelationship("Relative");
        contact.setPhoneNumber("07000 000000");

        return contact;
    }

    private PatientRecord patientRecord(UUID id) {
        PatientRecord patientRecord = new PatientRecord();

        ReflectionTestUtils.setField(patientRecord, "id", id);

        return patientRecord;
    }
}