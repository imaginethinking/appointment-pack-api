package net.imaginethinking.appointmentpack.pack.generation;

import net.imaginethinking.appointmentpack.pack.AppointmentPackItemType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    public AppointmentPackGenerationData {
        selectedItems = List.copyOf(selectedItems);
    }

    public record SelectedItem(
            AppointmentPackItemType resourceType,
            UUID resourceId,
            int displayOrder
    ) {
    }
}