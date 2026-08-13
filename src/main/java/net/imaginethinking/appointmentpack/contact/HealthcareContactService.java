package net.imaginethinking.appointmentpack.contact;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.address.Address;
import net.imaginethinking.appointmentpack.address.AddressRequest;
import net.imaginethinking.appointmentpack.patientcareraccess.PatientAccessControlService;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HealthcareContactService {

    private final HealthcareContactRepository healthcareContactRepository;
    private final PatientRecordRepository patientRecordRepository;
    private final PatientAccessControlService patientAccessControlService;

    @Transactional
    public HealthcareContactResponse createHealthcareContact(
            UUID authenticatedUserId,
            UUID patientRecordId,
            CreateHealthcareContactRequest request) {
        PatientRecord patientRecord = findPatientRecord(patientRecordId);

        patientAccessControlService.requirePermission(authenticatedUserId, patientRecord, ContactPermission.EDIT);

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

        return HealthcareContactResponse.from(savedContact);
    }

    @Transactional(readOnly = true)
    public List<HealthcareContactResponse> getHealthcareContacts(UUID authenticatedUserId, UUID patientRecordId) {
        PatientRecord patientRecord = findPatientRecord(patientRecordId);

        patientAccessControlService.requirePermission(authenticatedUserId, patientRecord, ContactPermission.VIEW);

        return healthcareContactRepository.findAllByPatientRecord_IdAndArchivedAtIsNullOrderByNameAsc(patientRecordId)
                .stream()
                .map(HealthcareContactResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public HealthcareContactResponse getHealthcareContact(UUID authenticatedUserId, UUID healthcareContactId) {
        HealthcareContact contact = findAvailableContact(healthcareContactId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                contact.getPatientRecord(),
                ContactPermission.VIEW);

        return HealthcareContactResponse.from(contact);
    }

    @Transactional
    public HealthcareContactResponse updateHealthcareContact(
            UUID authenticatedUserId,
            UUID healthcareContactId,
            UpdateHealthcareContactRequest request) {
        HealthcareContact contact = findAvailableContact(healthcareContactId);

        patientAccessControlService.requirePermission(
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

        return HealthcareContactResponse.from(contact);
    }

    @Transactional
    public HealthcareContactResponse archiveHealthcareContact(UUID authenticatedUserId, UUID healthcareContactId) {
        HealthcareContact contact = findContact(healthcareContactId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                contact.getPatientRecord(),
                ContactPermission.EDIT);

        contact.archive();

        return HealthcareContactResponse.from(contact);
    }

    private PatientRecord findPatientRecord(UUID patientRecordId) {
        return patientRecordRepository.findById(patientRecordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient record not found"));
    }

    private HealthcareContact findContact(UUID healthcareContactId) {
        return healthcareContactRepository.findById(healthcareContactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Healthcare contact not found"));
    }

    private HealthcareContact findAvailableContact(UUID healthcareContactId) {
        HealthcareContact contact = findContact(healthcareContactId);

        if (contact.isArchived()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Healthcare contact not found");
        }

        return contact;
    }

    private void applyValues(
            HealthcareContact contact,
            String name,
            String role,
            String organisation,
            String phoneNumber,
            String email,
            AddressRequest address,
            String notes) {
        contact.setName(name.trim());
        contact.setRole(normaliseOptionalValue(role));
        contact.setOrganisation(normaliseOptionalValue(organisation));
        contact.setPhoneNumber(normaliseOptionalValue(phoneNumber));
        contact.setEmail(normaliseOptionalValue(email));
        contact.setAddress(toAddress(address));
        contact.setNotes(normaliseOptionalValue(notes));
    }

    private Address toAddress(AddressRequest request) {
        if (request == null) {
            return null;
        }

        Address address = new Address();

        address.setAddressLine1(request.addressLine1().trim());
        address.setAddressLine2(normaliseOptionalValue(request.addressLine2()));
        address.setTownCity(request.townCity().trim());
        address.setCounty(normaliseOptionalValue(request.county()));
        address.setPostcode(request.postcode().trim());
        address.setCountry(request.country().trim());

        return address;
    }

    private String normaliseOptionalValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}