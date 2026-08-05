package net.imaginethinking.appointmentpack.medicalhistory;

import net.imaginethinking.appointmentpack.document.DocumentType;

import java.time.LocalDate;
import java.util.UUID;

public record MedicalHistoryEntryResponse(
        UUID id,
        String title,
        String summary,
        LocalDate entryDate,
        MedicalHistorySourceType sourceType,
        UUID sourceDocumentId,
        DocumentType sourceDocumentType
) {
}