package net.imaginethinking.appointmentpack.pack;

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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AppointmentPackController {

    private final AppointmentPackService appointmentPackService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @GetMapping("/patient-records/{patientRecordId}/appointment-packs")
    public ResponseEntity<List<AppointmentPackResponse>> getAppointmentPacks(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(appointmentPackService.getAppointmentPacks(authenticatedUserId, patientRecordId));
    }

    @GetMapping("/appointment-packs/{appointmentPackId}")
    public ResponseEntity<AppointmentPackResponse> getAppointmentPack(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentPackId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(appointmentPackService.getAppointmentPack(authenticatedUserId, appointmentPackId));
    }

    @GetMapping("/appointment-packs/{appointmentPackId}/file")
    public ResponseEntity<Resource> downloadAppointmentPack(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentPackId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        AppointmentPackDownload download = appointmentPackService.downloadAppointmentPack(
                authenticatedUserId,
                appointmentPackId);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.resource());
    }

    @PatchMapping("/appointment-packs/{appointmentPackId}/archive")
    public ResponseEntity<AppointmentPackResponse> archiveAppointmentPack(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentPackId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(appointmentPackService.archiveAppointmentPack(authenticatedUserId, appointmentPackId));
    }
}