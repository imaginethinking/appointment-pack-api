package net.imaginethinking.appointmentpack.contact;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents emergency contact information returned by the API.
 */
public record EmergencyContactResponse(
        UUID id,
        UUID patientRecordId,
        String name,
        String relationship,
        String phoneNumber,
        String alternativePhoneNumber,
        String email,
        String notes,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Builds the emergency contact response from the supplied emergency contact.
     */
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
                contact.getArchivedAt(),
                contact.getCreatedAt(),
                contact.getUpdatedAt()
        );
    }
}