package net.imaginethinking.appointmentpack.audit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/patient-records/{patientRecordId}/audit-events")
@RequiredArgsConstructor
public class PatientAuditController {

    private final PatientAuditService patientAuditService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @GetMapping
    public ResponseEntity<PatientAuditPageResponse> getAuditEvents(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must not be negative")
            int page,
            @RequestParam(defaultValue = "50")
            @Min(value = 1, message = "Page size must be at least 1")
            @Max(value = 100, message = "Page size must not exceed 100")
            int size
    ) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        PatientAuditPageResponse response =
                patientAuditService.getAuditEvents(
                        authenticatedUserId,
                        patientRecordId,
                        page,
                        size
                );

        return ResponseEntity.ok(response);
    }
}