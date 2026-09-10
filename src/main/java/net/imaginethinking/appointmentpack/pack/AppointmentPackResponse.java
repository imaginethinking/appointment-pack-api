package net.imaginethinking.appointmentpack.pack;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Represents Appointment Pack information returned by the API.
 */
public record AppointmentPackResponse(
        UUID id,
        UUID patientRecordId,
        UUID appointmentId,
        String title,
        String notes,
        UUID generatedByUserId,
        Instant generatedAt,
        String fileName,
        long fileSize,
        Instant archivedAt,
        List<ItemResponse> items
) {

    /**
     * Builds the Appointment Pack response from the supplied Appointment Pack.
     */
    public static AppointmentPackResponse from(AppointmentPack appointmentPack) {
        List<ItemResponse> items = appointmentPack
                .getItems()
                .stream()
                .sorted(Comparator.comparingInt(
                        AppointmentPackItem::getDisplayOrder
                ))
                .map(ItemResponse::from)
                .toList();

        return new AppointmentPackResponse(
                appointmentPack.getId(),
                appointmentPack.getPatientRecord().getId(),
                appointmentPack.getAppointment().getId(),
                appointmentPack.getTitle(),
                appointmentPack.getNotes(),
                appointmentPack.getGeneratedBy().getId(),
                appointmentPack.getGeneratedAt(),
                appointmentPack.getFileName(),
                appointmentPack.getFileSize(),
                appointmentPack.getArchivedAt(),
                items
        );
    }

    /**
     * Represents item information returned by the API.
     */
    public record ItemResponse(
            AppointmentPackItemType resourceType,
            UUID resourceId,
            int displayOrder
    ) {

        /**
         * Builds the item response from the supplied Appointment Pack item.
         */
        public static ItemResponse from(
                AppointmentPackItem item
        ) {
            return new ItemResponse(
                    item.getResourceType(),
                    item.getResourceId(),
                    item.getDisplayOrder()
            );
        }
    }
}