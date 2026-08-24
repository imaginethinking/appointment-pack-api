package net.imaginethinking.appointmentpack.pack;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import net.imaginethinking.appointmentpack.pack.generation.AppointmentPackGenerationData;
import net.imaginethinking.appointmentpack.pack.generation.AppointmentPackGenerationDataService;
import net.imaginethinking.appointmentpack.pack.generation.AppointmentPackPdfRenderer;
import net.imaginethinking.appointmentpack.pack.storage.AppointmentPackStorageService;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentPackService {

    private final AppointmentPackRepository appointmentPackRepository;
    private final PatientRecordAccessService patientRecordAccessService;
    private final AppointmentPackStorageService appointmentPackStorageService;
    private final AppointmentPackGenerationDataService generationDataService;
    private final AppointmentPackPdfRenderer pdfRenderer;
    private final AppointmentPackPersistenceService persistenceService;
    private final AppEventPublisher appEventPublisher;

    public AppointmentPackResponse generateAppointmentPack(
            UUID authenticatedUserId,
            UUID patientRecordId,
            AppointmentPackGenerationRequest request) {
        AppointmentPackGenerationData generationData = generationDataService.prepare(
                authenticatedUserId,
                patientRecordId,
                request);

        byte[] pdfBytes = pdfRenderer.render(generationData.renderModel());

        if (pdfBytes.length == 0) {
            throw new IllegalStateException("Generated appointment pack PDF was empty");
        }

        String storagePath = appointmentPackStorageService.store(pdfBytes);

        try {
            return persistenceService.persist(authenticatedUserId, generationData, storagePath, pdfBytes.length);
        } catch (RuntimeException exception) {
            try {
                appointmentPackStorageService.delete(storagePath);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<AppointmentPackResponse> getAppointmentPacks(UUID authenticatedUserId, UUID patientRecordId) {
        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecordId,
                AppointmentPackPermission.VIEW);

        return appointmentPackRepository.findAllByPatientRecord_IdAndArchivedAtIsNullOrderByGeneratedAtDesc(
                patientRecordId).stream().map(AppointmentPackResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentPackResponse getAppointmentPack(UUID authenticatedUserId, UUID appointmentPackId) {
        AppointmentPack appointmentPack = requireAvailableAppointmentPackAccess(
                authenticatedUserId,
                appointmentPackId);

        return AppointmentPackResponse.from(appointmentPack);
    }

    @Transactional(readOnly = true)
    public AppointmentPackFile previewAppointmentPack(UUID authenticatedUserId, UUID appointmentPackId) {
        AppointmentPack appointmentPack = requireAvailableAppointmentPackAccess(
                authenticatedUserId,
                appointmentPackId);

        return loadFile(appointmentPack);
    }

    @Transactional
    public AppointmentPackFile downloadAppointmentPack(UUID authenticatedUserId, UUID appointmentPackId) {
        AppointmentPack appointmentPack = requireAvailableAppointmentPackAccess(
                authenticatedUserId,
                appointmentPackId);

        AppointmentPackFile file = loadFile(appointmentPack);

        publishActivity(authenticatedUserId, appointmentPack, PatientActivityAction.DOWNLOADED);

        return file;
    }

    @Transactional
    public AppointmentPackResponse archiveAppointmentPack(UUID authenticatedUserId, UUID appointmentPackId) {
        AppointmentPack appointmentPack = findAppointmentPack(appointmentPackId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                appointmentPack.getPatientRecord(),
                AppointmentPackPermission.CREATE);

        if (!appointmentPack.isArchived()) {
            appointmentPack.archive();

            publishActivity(authenticatedUserId, appointmentPack, PatientActivityAction.ARCHIVED);
        }

        return AppointmentPackResponse.from(appointmentPack);
    }

    private AppointmentPack requireAvailableAppointmentPackAccess(
            UUID authenticatedUserId,
            UUID appointmentPackId) {
        AppointmentPack appointmentPack = findAvailableAppointmentPack(appointmentPackId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                appointmentPack.getPatientRecord(),
                AppointmentPackPermission.VIEW);

        return appointmentPack;
    }

    private AppointmentPackFile loadFile(AppointmentPack appointmentPack) {
        return new AppointmentPackFile(
                appointmentPackStorageService.load(appointmentPack.getStoragePath()),
                appointmentPack.getFileName(),
                appointmentPack.getContentType(),
                appointmentPack.getFileSize());
    }

    private void publishActivity(
            UUID authenticatedUserId,
            AppointmentPack appointmentPack,
            PatientActivityAction action) {
        appEventPublisher.publish(PatientActivityEvent.create(
                authenticatedUserId,
                appointmentPack.getPatientRecord().getId(),
                PatientResourceType.APPOINTMENT_PACK,
                appointmentPack.getId(),
                action));
    }

    private AppointmentPack findAppointmentPack(UUID appointmentPackId) {
        return appointmentPackRepository.findById(appointmentPackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment pack not found"));
    }

    private AppointmentPack findAvailableAppointmentPack(UUID appointmentPackId) {
        AppointmentPack appointmentPack = findAppointmentPack(appointmentPackId);

        if (appointmentPack.isArchived()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment pack not found");
        }

        return appointmentPack;
    }
}