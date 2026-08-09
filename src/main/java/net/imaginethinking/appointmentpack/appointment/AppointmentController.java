package net.imaginethinking.appointmentpack.appointment;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @GetMapping("/patient-records/{patientRecordId}/appointments")
    public ResponseEntity<List<AppointmentResponse>> getAppointments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        List<AppointmentResponse> response = appointmentService.getAppointments(authenticatedUserId, patientRecordId);

        return ResponseEntity.ok(response);
    }
}