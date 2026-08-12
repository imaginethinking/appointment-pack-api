package net.imaginethinking.appointmentpack.contact;

import java.time.Instant;
import java.util.UUID;

public record EmergencyContactResponse(
        UUID id,
        UUID patientRecordId,
        String name,
        String relationship,
        String phoneNumber,
        String alternativePhoneNumber,
        String email,
        String notes,
        boolean archived,
        Instant createdAt,
        Instant updatedAt
) {

    public static EmergencyContactResponse from(EmergencyContact contact) {
        return new EmergencyContactResponse(
                contact.getId(),
                contact.getPatientRecord().getId(),
                contact.getName(),
                contact.getRelationship(),
                contact.getPhoneNumber(),
                contact.getAlternativePhoneNumber(),
                contact.getEmail(),
                contact.getNotes(),
                contact.isArchived(),
                contact.getCreatedAt(),
                contact.getUpdatedAt()
        );
    }
}