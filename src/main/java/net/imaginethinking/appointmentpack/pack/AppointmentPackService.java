package net.imaginethinking.appointmentpack.pack;

import lombok.RequiredArgsConstructor;
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
        AppointmentPack appointmentPack = findAvailableAppointmentPack(appointmentPackId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                appointmentPack.getPatientRecord(),
                AppointmentPackPermission.VIEW);

        return AppointmentPackResponse.from(appointmentPack);
    }

    @Transactional(readOnly = true)
    public AppointmentPackDownload downloadAppointmentPack(UUID authenticatedUserId, UUID appointmentPackId) {
        AppointmentPack appointmentPack = findAvailableAppointmentPack(appointmentPackId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                appointmentPack.getPatientRecord(),
                AppointmentPackPermission.VIEW);

        return new AppointmentPackDownload(
                appointmentPackStorageService.load(appointmentPack.getStoragePath()),
                appointmentPack.getFileName(),
                appointmentPack.getContentType(),
                appointmentPack.getFileSize());
    }

    @Transactional
    public AppointmentPackResponse archiveAppointmentPack(UUID authenticatedUserId, UUID appointmentPackId) {
        AppointmentPack appointmentPack = findAppointmentPack(appointmentPackId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                appointmentPack.getPatientRecord(),
                AppointmentPackPermission.CREATE);

        appointmentPack.archive();

        return AppointmentPackResponse.from(appointmentPack);
    }

    private AppointmentPack findAppointmentPack(
            UUID appointmentPackId) {
        return appointmentPackRepository.findById(appointmentPackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment pack not found"));
    }

    private AppointmentPack findAvailableAppointmentPack(
            UUID appointmentPackId) {
        AppointmentPack appointmentPack = findAppointmentPack(appointmentPackId);

        if (appointmentPack.isArchived()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment pack not found");
        }

        return appointmentPack;
    }
}