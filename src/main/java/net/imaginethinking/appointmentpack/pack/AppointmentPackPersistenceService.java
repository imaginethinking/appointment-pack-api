package net.imaginethinking.appointmentpack.pack;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.appointment.Appointment;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import net.imaginethinking.appointmentpack.pack.generation.AppointmentPackGenerationData;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.user.User;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Saves a generated Appointment Pack and records the items that were included in the PDF.
 */
@Service
@RequiredArgsConstructor
public class AppointmentPackPersistenceService {

    private final AppointmentPackRepository appointmentPackRepository;
    private final EntityManager entityManager;
    private final AppEventPublisher appEventPublisher;

    /**
     * Saves the generated pack and records the type and ID of every item included in the PDF.
     */
    @Transactional
    public AppointmentPackResponse persist(
            UUID authenticatedUserId,
            AppointmentPackGenerationData generationData,
            String storagePath,
            long fileSize) {
        AppointmentPack appointmentPack = new AppointmentPack();

        appointmentPack.setPatientRecord(entityManager.getReference(PatientRecord.class, generationData.patientRecordId()));

        appointmentPack.setAppointment(entityManager.getReference(Appointment.class, generationData.appointmentId()));
        appointmentPack.setTitle(generationData.title());
        appointmentPack.setNotes(generationData.notes());
        appointmentPack.setGeneratedBy(entityManager.getReference(User.class, authenticatedUserId));
        appointmentPack.setGeneratedAt(generationData.generatedAt());
        appointmentPack.setFileName(generationData.fileName());
        appointmentPack.setStoredFileName(Path.of(storagePath).getFileName().toString());
        appointmentPack.setStoragePath(storagePath);
        appointmentPack.setContentType(MediaType.APPLICATION_PDF_VALUE);
        appointmentPack.setFileSize(fileSize);

        // Keep the selected resource IDs with the pack so its contents can still be traced after source records change.
        for (AppointmentPackGenerationData.SelectedItem selectedItem : generationData.selectedItems()) {

            AppointmentPackItem item = new AppointmentPackItem();

            item.setAppointmentPack(appointmentPack);
            item.setResourceType(selectedItem.resourceType());
            item.setResourceId(selectedItem.resourceId());
            item.setDisplayOrder(selectedItem.displayOrder());

            appointmentPack.getItems().add(item);
        }

        AppointmentPack savedAppointmentPack = appointmentPackRepository.saveAndFlush(appointmentPack);

        appEventPublisher.publish(PatientActivityEvent.create(
                authenticatedUserId,
                generationData.patientRecordId(),
                PatientResourceType.APPOINTMENT_PACK,
                savedAppointmentPack.getId(),
                PatientActivityAction.GENERATED));

        return AppointmentPackResponse.from(savedAppointmentPack);
    }
}