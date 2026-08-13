package net.imaginethinking.appointmentpack.contact;

import net.imaginethinking.appointmentpack.address.AddressResponse;

import java.time.Instant;
import java.util.UUID;

public record HealthcareContactResponse(
        UUID id,
        UUID patientRecordId,
        String name,
        String role,
        String organisation,
        String phoneNumber,
        String email,
        AddressResponse address,
        String notes,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static HealthcareContactResponse from(HealthcareContact contact) {
        return new HealthcareContactResponse(
                contact.getId(),
                contact.getPatientRecord().getId(),
                contact.getName(),
                contact.getRole(),
                contact.getOrganisation(),
                contact.getPhoneNumber(),
                contact.getEmail(),
                AddressResponse.from(contact.getAddress()),
                contact.getNotes(),
                contact.getArchivedAt(),
                contact.getCreatedAt(),
                contact.getUpdatedAt()
        );
    }
}