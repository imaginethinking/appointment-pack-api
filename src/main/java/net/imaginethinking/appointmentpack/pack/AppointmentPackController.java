package net.imaginethinking.appointmentpack.pack;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Handles requests for generating, viewing, previewing, downloading and archiving Appointment Packs.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AppointmentPackController {

    private final AppointmentPackService appointmentPackService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    /**
     * Generates the requested Appointment Pack using the submitted selections.
     */
    @PostMapping("/patient-records/{patientRecordId}/appointment-packs")
    public ResponseEntity<AppointmentPackResponse> generateAppointmentPack(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId,
            @Valid @RequestBody AppointmentPackGenerationRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        AppointmentPackResponse response = appointmentPackService.generateAppointmentPack(
                authenticatedUserId,
                patientRecordId,
                request);

        return ResponseEntity.created(URI.create("/api/v1/appointment-packs/" + response.id())).body(response);
    }

    /**
     * Returns the requested Appointment Packs for the signed in user.
     */
    @GetMapping("/patient-records/{patientRecordId}/appointment-packs")
    public ResponseEntity<List<AppointmentPackResponse>> getAppointmentPacks(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(appointmentPackService.getAppointmentPacks(authenticatedUserId, patientRecordId));
    }

    /**
     * Returns the requested Appointment Pack for the signed in user.
     */
    @GetMapping("/appointment-packs/{appointmentPackId}")
    public ResponseEntity<AppointmentPackResponse> getAppointmentPack(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentPackId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(appointmentPackService.getAppointmentPack(authenticatedUserId, appointmentPackId));
    }

    /**
     * Returns the requested Appointment Pack PDF for inline viewing.
     */
    @GetMapping("/appointment-packs/{appointmentPackId}/preview")
    public ResponseEntity<Resource> previewAppointmentPack(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentPackId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        AppointmentPackFile file = appointmentPackService.previewAppointmentPack(
                authenticatedUserId,
                appointmentPackId);

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();

        return fileResponse(file, disposition);
    }

    /**
     * Returns the requested Appointment Pack file as a download.
     */
    @GetMapping("/appointment-packs/{appointmentPackId}/file")
    public ResponseEntity<Resource> downloadAppointmentPack(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentPackId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        AppointmentPackFile file = appointmentPackService.downloadAppointmentPack(
                authenticatedUserId,
                appointmentPackId);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();

        return fileResponse(file, disposition);
    }

    /**
     * Archives the requested Appointment Pack.
     */
    @PatchMapping("/appointment-packs/{appointmentPackId}/archive")
    public ResponseEntity<AppointmentPackResponse> archiveAppointmentPack(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentPackId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(appointmentPackService.archiveAppointmentPack(authenticatedUserId, appointmentPackId));
    }

    /**
     * Builds the PDF response with the saved content type, file name and requested content disposition.
     */
    private ResponseEntity<Resource> fileResponse(
            AppointmentPackFile file,
            ContentDisposition disposition) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }
}