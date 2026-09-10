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

/**
 * Generates, loads, previews, downloads and archives Appointment Packs for patients the current user can access.
 */
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

    /**
     * Checks pack creation access, prepares and stores the PDF, then saves the pack and its selected item details.
     *
     * @param authenticatedUserId user creating the Appointment Pack
     * @param patientRecordId patient record the pack is being created for
     * @param request appointment and patient information selected for the pack
     * @return the saved Appointment Pack details
     */
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

        // Delete the PDF again if its database record cannot be saved so an unused pack file is not left behind.
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

    /**
     * Checks view access and returns the active Appointment Packs for the selected patient.
     */
    @Transactional(readOnly = true)
    public List<AppointmentPackResponse> getAppointmentPacks(UUID authenticatedUserId, UUID patientRecordId) {
        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecordId,
                AppointmentPackPermission.VIEW);

        return appointmentPackRepository.findAllByPatientRecord_IdAndArchivedAtIsNullOrderByGeneratedAtDesc(
                patientRecordId).stream().map(AppointmentPackResponse::from).toList();
    }

    /**
     * Loads an active Appointment Pack and checks that the current user can view its patient record.
     */
    @Transactional(readOnly = true)
    public AppointmentPackResponse getAppointmentPack(UUID authenticatedUserId, UUID appointmentPackId) {
        AppointmentPack appointmentPack = requireAvailableAppointmentPackAccess(
                authenticatedUserId,
                appointmentPackId);

        return AppointmentPackResponse.from(appointmentPack);
    }

    /**
     * Checks view access and returns the stored PDF for inline preview without recording a download event.
     */
    @Transactional(readOnly = true)
    public AppointmentPackFile previewAppointmentPack(UUID authenticatedUserId, UUID appointmentPackId) {
        AppointmentPack appointmentPack = requireAvailableAppointmentPackAccess(
                authenticatedUserId,
                appointmentPackId);

        return loadFile(appointmentPack);
    }

    /**
     * Checks view access, loads the stored PDF and records the patient activity for the download.
     */
    @Transactional
    public AppointmentPackFile downloadAppointmentPack(UUID authenticatedUserId, UUID appointmentPackId) {
        AppointmentPack appointmentPack = requireAvailableAppointmentPackAccess(
                authenticatedUserId,
                appointmentPackId);

        AppointmentPackFile file = loadFile(appointmentPack);

        publishActivity(authenticatedUserId, appointmentPack, PatientActivityAction.DOWNLOADED);

        return file;
    }

    /**
     * Checks pack creation access and archives the pack if it is still active.
     */
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

    /**
     * Loads an active Appointment Pack and checks the requested patient permission before returning it.
     */
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

    /**
     * Loads the stored PDF and keeps its saved file information together for the response.
     */
    private AppointmentPackFile loadFile(AppointmentPack appointmentPack) {
        return new AppointmentPackFile(
                appointmentPackStorageService.load(appointmentPack.getStoragePath()),
                appointmentPack.getFileName(),
                appointmentPack.getContentType(),
                appointmentPack.getFileSize());
    }

    /**
     * Publishes the activity event for the completed change.
     */
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

    /**
     * Loads an Appointment Pack by ID or returns not found when it does not exist.
     */
    private AppointmentPack findAppointmentPack(UUID appointmentPackId) {
        return appointmentPackRepository.findById(appointmentPackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment pack not found"));
    }

    /**
     * Loads an Appointment Pack and treats archived packs as not found.
     */
    private AppointmentPack findAvailableAppointmentPack(UUID appointmentPackId) {
        AppointmentPack appointmentPack = findAppointmentPack(appointmentPackId);

        if (appointmentPack.isArchived()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment pack not found");
        }

        return appointmentPack;
    }
}