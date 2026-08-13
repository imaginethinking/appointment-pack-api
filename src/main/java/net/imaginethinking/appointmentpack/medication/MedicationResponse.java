package net.imaginethinking.appointmentpack.medication;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MedicationResponse(
        UUID id,
        UUID patientRecordId,
        String name,
        String dose,
        String form,
        String instructions,
        LocalDate startDate,
        LocalDate endDate,
        String notes,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static MedicationResponse from(Medication medication) {
        return new MedicationResponse(
                medication.getId(),
                medication.getPatientRecord().getId(),
                medication.getName(),
                medication.getDose(),
                medication.getForm(),
                medication.getInstructions(),
                medication.getStartDate(),
                medication.getEndDate(),
                medication.getNotes(),
                medication.getArchivedAt(),
                medication.getCreatedAt(),
                medication.getUpdatedAt()
        );
    }
}