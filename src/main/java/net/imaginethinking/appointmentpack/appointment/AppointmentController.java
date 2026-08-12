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
            @Valid @RequestBody AppointmentRequest request) {
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

        List<AppointmentResponse> response = appointmentService.getAppointments(authenticatedUserId, patientRecordId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/appointments/{appointmentId}")
    public ResponseEntity<AppointmentResponse> getAppointment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        AppointmentResponse response = appointmentService.getAppointment(authenticatedUserId, appointmentId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/appointments/{appointmentId}")
    public ResponseEntity<AppointmentResponse> updateAppointment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentId,
            @Valid @RequestBody AppointmentRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        AppointmentResponse response = appointmentService.updateAppointment(
                authenticatedUserId,
                appointmentId,
                request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/appointments/{appointmentId}/archive")
    public ResponseEntity<AppointmentResponse> archiveAppointment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        AppointmentResponse response = appointmentService.archiveAppointment(authenticatedUserId, appointmentId);

        return ResponseEntity.ok(response);
    }
}