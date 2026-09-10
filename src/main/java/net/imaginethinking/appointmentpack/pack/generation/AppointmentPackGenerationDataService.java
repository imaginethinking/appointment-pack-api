package net.imaginethinking.appointmentpack.pack.generation;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.appointment.Appointment;
import net.imaginethinking.appointmentpack.common.TextNormalizer;
import net.imaginethinking.appointmentpack.pack.AppointmentPackGenerationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * Prepares the selected patient data, title, file name and PDF content needed to save an Appointment Pack.
 */
@Service
@RequiredArgsConstructor
public class AppointmentPackGenerationDataService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.UK);

    private final AppointmentPackSelectionService selectionService;
    private final AppointmentPackRenderModelFactory renderModelFactory;

    /**
     * Loads the selected patient information, builds the render model and PDF, then returns everything needed to
     * persist the pack.
     */
    @Transactional(readOnly = true)
    public AppointmentPackGenerationData prepare(
            UUID authenticatedUserId,
            UUID patientRecordId,
            AppointmentPackGenerationRequest request) {
        AppointmentPackSelection selection = selectionService.select(authenticatedUserId, patientRecordId, request);

        Instant generatedAt = Instant.now();

        String title = resolveTitle(request.title(), selection.appointment());

        String notes = TextNormalizer.stripToNull(request.notes());

        AppointmentPackRenderModel renderModel = renderModelFactory.create(selection, title, notes, generatedAt);

        return new AppointmentPackGenerationData(
                selection.patientRecord().getId(),
                selection.appointment().getId(),
                title,
                notes,
                generatedAt,
                createFileName(title),
                renderModel,
                selection.selectedItems());
    }

    /**
     * Uses the entered title when present or creates one from the selected appointment when it is blank.
     */
    private String resolveTitle(String requestedTitle, Appointment appointment) {
        String title = TextNormalizer.stripToNull(requestedTitle);

        if (title != null) {
            return title;
        }

        String service = TextNormalizer.stripToNull(appointment.getService());

        String defaultTitle = service == null ? "Appointment Pack" : service + " Appointment Pack";

        defaultTitle += " – " + DATE_FORMATTER.format(appointment.getDate());

        if (defaultTitle.length() > 250) {
            return TextNormalizer.strip(defaultTitle.substring(0, 250));
        }

        return defaultTitle;
    }

    /**
     * Creates a safe PDF file name from the pack title and a random suffix.
     */
    private String createFileName(String title) {
        String normalisedTitle = Normalizer.normalize(title, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .toLowerCase(Locale.ROOT);

        if (normalisedTitle.isBlank()) {
            normalisedTitle = "appointment-pack";
        }

        if (normalisedTitle.length() > 120) {
            normalisedTitle = normalisedTitle.substring(0, 120);
        }

        return normalisedTitle + ".pdf";
    }
}