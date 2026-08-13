package net.imaginethinking.appointmentpack.contact;

import lombok.RequiredArgsConstructor;
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
public class EmergencyContactService {

    private final EmergencyContactRepository emergencyContactRepository;
    private final PatientRecordRepository patientRecordRepository;
    private final PatientAccessControlService patientAccessControlService;

    @Transactional
    public EmergencyContactResponse createEmergencyContact(
            UUID authenticatedUserId,
            UUID patientRecordId,
            CreateEmergencyContactRequest request) {
        PatientRecord patientRecord = findPatientRecord(patientRecordId);

        patientAccessControlService.requirePermission(authenticatedUserId, patientRecord, ContactPermission.EDIT);

        EmergencyContact contact = new EmergencyContact();

        contact.setPatientRecord(patientRecord);

        applyValues(
                contact,
                request.name(),
                request.relationship(),
                request.phoneNumber(),
                request.alternativePhoneNumber(),
                request.email(),
                request.notes());

        EmergencyContact savedContact = emergencyContactRepository.save(contact);

        return EmergencyContactResponse.from(savedContact);
    }

    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> getEmergencyContacts(UUID authenticatedUserId, UUID patientRecordId) {
        PatientRecord patientRecord = findPatientRecord(patientRecordId);

        patientAccessControlService.requirePermission(authenticatedUserId, patientRecord, ContactPermission.VIEW);

        return emergencyContactRepository.findAllByPatientRecord_IdAndArchivedAtIsNullOrderByNameAsc(patientRecordId)
                .stream()
                .map(EmergencyContactResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmergencyContactResponse getEmergencyContact(UUID authenticatedUserId, UUID emergencyContactId) {
        EmergencyContact contact = findAvailableContact(emergencyContactId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                contact.getPatientRecord(),
                ContactPermission.VIEW);

        return EmergencyContactResponse.from(contact);
    }

    @Transactional
    public EmergencyContactResponse updateEmergencyContact(
            UUID authenticatedUserId,
            UUID emergencyContactId,
            UpdateEmergencyContactRequest request) {
        EmergencyContact contact = findAvailableContact(emergencyContactId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                contact.getPatientRecord(),
                ContactPermission.EDIT);

        applyValues(
                contact,
                request.name(),
                request.relationship(),
                request.phoneNumber(),
                request.alternativePhoneNumber(),
                request.email(),
                request.notes());

        return EmergencyContactResponse.from(contact);
    }

    @Transactional
    public EmergencyContactResponse archiveEmergencyContact(UUID authenticatedUserId, UUID emergencyContactId) {
        EmergencyContact contact = findContact(emergencyContactId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                contact.getPatientRecord(),
                ContactPermission.EDIT);

        contact.archive();

        return EmergencyContactResponse.from(contact);
    }

    private PatientRecord findPatientRecord(UUID patientRecordId) {
        return patientRecordRepository.findById(patientRecordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient record not found"));
    }

    private EmergencyContact findContact(UUID emergencyContactId) {
        return emergencyContactRepository.findById(emergencyContactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency contact not found"));
    }

    private EmergencyContact findAvailableContact(UUID emergencyContactId) {
        EmergencyContact contact = findContact(emergencyContactId);

        if (contact.isArchived()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency contact not found");
        }

        return contact;
    }

    private void applyValues(
            EmergencyContact contact,
            String name,
            String relationship,
            String phoneNumber,
            String alternativePhoneNumber,
            String email,
            String notes) {
        contact.setName(name.trim());
        contact.setRelationship(relationship.trim());
        contact.setPhoneNumber(phoneNumber.trim());
        contact.setAlternativePhoneNumber(normaliseOptionalValue(alternativePhoneNumber));
        contact.setEmail(normaliseOptionalValue(email));
        contact.setNotes(normaliseOptionalValue(notes));
    }

    private String normaliseOptionalValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}