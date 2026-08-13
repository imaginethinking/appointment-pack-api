package net.imaginethinking.appointmentpack.appointment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @PostMapping("/patient-records/{patientRecordId}/appointments")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId,
            @Valid @RequestBody CreateAppointmentRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        AppointmentResponse response = appointmentService.createAppointment(
                authenticatedUserId,
                patientRecordId,
                request);

        return ResponseEntity.created(URI.create("/api/v1/appointments/" + response.id())).body(response);
    }

    @GetMapping("/patient-records/{patientRecordId}/appointments")
    public ResponseEntity<List<AppointmentResponse>> getAppointments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(appointmentService.getAppointments(authenticatedUserId, patientRecordId));
    }

    @GetMapping("/appointments/{appointmentId}")
    public ResponseEntity<AppointmentResponse> getAppointment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(appointmentService.getAppointment(authenticatedUserId, appointmentId));
    }

    @PutMapping("/appointments/{appointmentId}")
    public ResponseEntity<AppointmentResponse> updateAppointment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentId,
            @Valid @RequestBody UpdateAppointmentRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(appointmentService.updateAppointment(authenticatedUserId, appointmentId, request));
    }

    @PatchMapping("/appointments/{appointmentId}/archive")
    public ResponseEntity<AppointmentResponse> archiveAppointment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(appointmentService.archiveAppointment(authenticatedUserId, appointmentId));
    }
}