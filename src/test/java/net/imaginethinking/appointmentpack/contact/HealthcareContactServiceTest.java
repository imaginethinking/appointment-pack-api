package net.imaginethinking.appointmentpack.contact;

import net.imaginethinking.appointmentpack.address.AddressRequest;
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
class HealthcareContactServiceTest {

    @Mock
    private HealthcareContactRepository healthcareContactRepository;

    @Mock
    private PatientRecordAccessService patientRecordAccessService;

    @Mock
    private AppEventPublisher appEventPublisher;

    private HealthcareContactService service;

    @BeforeEach
    void setUp() {
        service = new HealthcareContactService(
                healthcareContactRepository,
                patientRecordAccessService,
                appEventPublisher);
    }

    @Test
    void shouldCreateHealthcareContactAndNormaliseValues() {
        UUID userId = UUID.randomUUID();
        UUID patientRecordId = UUID.randomUUID();

        when(patientRecordAccessService.requireAccess(userId, patientRecordId, ContactPermission.EDIT)).thenReturn(
                patientRecord(patientRecordId));

        when(healthcareContactRepository.save(any(HealthcareContact.class))).thenAnswer(invocation -> {
            HealthcareContact contact = invocation.getArgument(0);

            ReflectionTestUtils.setField(contact, "id", UUID.randomUUID());

            return contact;
        });

        HealthcareContactResponse response = service.createHealthcareContact(
                userId, patientRecordId, new CreateHealthcareContactRequest(
                        " Dr Campbell ",
                        " GP ",
                        " Riverside Practice ",
                        " 01234 567890 ",
                        " doctor@example.com ",
                        new AddressRequest(" 10 Main Street ", null, " Glasgow ", null, " G1 1AA ", " Scotland "),
                        " Primary contact "));

        assertEquals("Dr Campbell", response.name());

        assertEquals("GP", response.role());

        assertEquals("Riverside Practice", response.organisation());

        assertEquals("10 Main Street", response.address().addressLine1());

        assertEquals("Primary contact", response.notes());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldUpdateHealthcareContact() {
        UUID userId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();

        HealthcareContact contact = healthcareContact(contactId);

        when(healthcareContactRepository.findById(contactId)).thenReturn(Optional.of(contact));

        HealthcareContactResponse response = service.updateHealthcareContact(
                userId,
                contactId,
                new UpdateHealthcareContactRequest(" Updated Contact ", " Consultant ", null, null, null, null, null));

        assertEquals("Updated Contact", response.name());

        assertEquals("Consultant", response.role());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldArchiveHealthcareContactIdempotently() {
        UUID userId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();

        HealthcareContact contact = healthcareContact(contactId);

        when(healthcareContactRepository.findById(contactId)).thenReturn(Optional.of(contact));

        HealthcareContactResponse first = service.archiveHealthcareContact(userId, contactId);

        HealthcareContactResponse second = service.archiveHealthcareContact(userId, contactId);

        assertNotNull(first.archivedAt());

        assertEquals(first.archivedAt(), second.archivedAt());

        verify(appEventPublisher, times(1)).publish(any());
    }

    @Test
    void shouldHideArchivedHealthcareContactFromNormalRetrieval() {
        UUID userId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();

        HealthcareContact contact = healthcareContact(contactId);

        contact.archive();

        when(healthcareContactRepository.findById(contactId)).thenReturn(Optional.of(contact));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getHealthcareContact(userId, contactId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private HealthcareContact healthcareContact(
            UUID id) {
        HealthcareContact contact = new HealthcareContact();

        ReflectionTestUtils.setField(contact, "id", id);

        contact.setPatientRecord(patientRecord(UUID.randomUUID()));

        contact.setName("Contact");

        return contact;
    }

    private PatientRecord patientRecord(UUID id) {
        PatientRecord patientRecord = new PatientRecord();

        ReflectionTestUtils.setField(patientRecord, "id", id);

        return patientRecord;
    }
}