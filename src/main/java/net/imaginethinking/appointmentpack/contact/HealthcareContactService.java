package net.imaginethinking.appointmentpack.contact;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.address.AddressMapper;
import net.imaginethinking.appointmentpack.address.AddressRequest;
import net.imaginethinking.appointmentpack.common.TextNormalizer;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Creates and updates healthcare contacts after checking access to the selected patient record.
 */
@Service
@RequiredArgsConstructor
public class HealthcareContactService {

    private final HealthcareContactRepository healthcareContactRepository;
    private final PatientRecordAccessService patientRecordAccessService;
    private final AppEventPublisher appEventPublisher;

    /**
     * Checks edit access before saving a new healthcare contact using the submitted values.
     */
    @Transactional
    public HealthcareContactResponse createHealthcareContact(
            UUID authenticatedUserId,
            UUID patientRecordId,
            CreateHealthcareContactRequest request) {
        PatientRecord patientRecord = patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecordId,
                ContactPermission.EDIT);

        HealthcareContact contact = new HealthcareContact();

        contact.setPatientRecord(patientRecord);

        applyValues(
                contact,
                request.name(),
                request.role(),
                request.organisation(),
                request.phoneNumber(),
                request.email(),
                request.address(),
                request.notes());

        HealthcareContact savedContact = healthcareContactRepository.save(contact);

        publishActivity(
                authenticatedUserId,
                savedContact,
                PatientActivityAction.CREATED);

        return HealthcareContactResponse.from(savedContact);
    }

    /**
     * Checks view access before returning the active healthcare contacts in name order.
     */
    @Transactional(readOnly = true)
    public List<HealthcareContactResponse> getHealthcareContacts(UUID authenticatedUserId, UUID patientRecordId) {
        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecordId,
                ContactPermission.VIEW);

        return healthcareContactRepository.findAllByPatientRecord_IdAndArchivedAtIsNullOrderByNameAsc(patientRecordId)
                .stream()
                .map(HealthcareContactResponse::from)
                .toList();
    }

    /**
     * Loads the requested healthcare contact and checks that the current user can view its patient record.
     */
    @Transactional(readOnly = true)
    public HealthcareContactResponse getHealthcareContact(UUID authenticatedUserId, UUID healthcareContactId) {
        HealthcareContact contact = findAvailableContact(healthcareContactId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                contact.getPatientRecord(),
                ContactPermission.VIEW);

        return HealthcareContactResponse.from(contact);
    }

    /**
     * Loads the current healthcare contact, checks edit access and applies the submitted changes.
     */
    @Transactional
    public HealthcareContactResponse updateHealthcareContact(
            UUID authenticatedUserId,
            UUID healthcareContactId,
            UpdateHealthcareContactRequest request) {
        HealthcareContact contact = findAvailableContact(healthcareContactId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                contact.getPatientRecord(),
                ContactPermission.EDIT);

        applyValues(
                contact,
                request.name(),
                request.role(),
                request.organisation(),
                request.phoneNumber(),
                request.email(),
                request.address(),
                request.notes());

        publishActivity(
                authenticatedUserId,
                contact,
                PatientActivityAction.UPDATED);

        return HealthcareContactResponse.from(contact);
    }

    /**
     * Checks access and archives the healthcare contact only when it is still active.
     */
    @Transactional
    public HealthcareContactResponse archiveHealthcareContact(UUID authenticatedUserId, UUID healthcareContactId) {
        HealthcareContact contact = findContact(healthcareContactId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                contact.getPatientRecord(),
                ContactPermission.EDIT);

        if (!contact.isArchived()) {
            contact.archive();

            publishActivity(
                    authenticatedUserId,
                    contact,
                    PatientActivityAction.ARCHIVED);
        }

        return HealthcareContactResponse.from(contact);
    }

    /**
     * Loads the contact or returns not found when it does not exist.
     */
    private HealthcareContact findContact(UUID healthcareContactId) {
        return healthcareContactRepository.findById(healthcareContactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Healthcare contact not found"));
    }

    /**
     * Loads the contact and treats an archived record as not found.
     */
    private HealthcareContact findAvailableContact(UUID healthcareContactId) {
        HealthcareContact contact = findContact(healthcareContactId);

        if (contact.isArchived()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Healthcare contact not found");
        }

        return contact;
    }

    /**
     * Publishes the activity event for the completed change.
     */
    private void publishActivity(
            UUID authenticatedUserId,
            HealthcareContact contact,
            PatientActivityAction action) {
        appEventPublisher.publish(PatientActivityEvent.create(
                authenticatedUserId,
                contact.getPatientRecord().getId(),
                PatientResourceType.HEALTHCARE_CONTACT,
                contact.getId(),
                action));
    }

    /**
     * Copies the submitted contact and address details onto the healthcare contact while trimming optional text.
     */
    private void applyValues(
            HealthcareContact contact,
            String name,
            String role,
            String organisation,
            String phoneNumber,
            String email,
            AddressRequest address,
            String notes) {
        contact.setName(TextNormalizer.strip(name));
        contact.setRole(TextNormalizer.stripToNull(role));
        contact.setOrganisation(TextNormalizer.stripToNull(organisation));
        contact.setPhoneNumber(TextNormalizer.stripToNull(phoneNumber));
        contact.setEmail(TextNormalizer.stripToNull(email));
        contact.setAddress(AddressMapper.toAddress(address));
        contact.setNotes(TextNormalizer.stripToNull(notes));
    }
}