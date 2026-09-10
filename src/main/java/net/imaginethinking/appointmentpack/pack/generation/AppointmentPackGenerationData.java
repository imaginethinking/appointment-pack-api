package net.imaginethinking.appointmentpack.pack.generation;

import net.imaginethinking.appointmentpack.pack.AppointmentPackItemType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Keeps the prepared PDF, metadata and selected items needed to save a generated Appointment Pack.
 */
public record AppointmentPackGenerationData(
        UUID patientRecordId,
        UUID appointmentId,
        String title,
        String notes,
        Instant generatedAt,
        String fileName,
        AppointmentPackRenderModel renderModel,
        List<SelectedItem> selectedItems
) {

    /**
     * Copies the selected item list so the prepared generation data stays unchanged while the pack is saved.
     */
    public AppointmentPackGenerationData {
        selectedItems = List.copyOf(selectedItems);
    }

    /**
     * Keeps the resource type and ID recorded for one item included in an Appointment Pack.
     */
    public record SelectedItem(
            AppointmentPackItemType resourceType,
            UUID resourceId,
            int displayOrder
    ) {
    }
}