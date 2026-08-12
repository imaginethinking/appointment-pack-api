package net.imaginethinking.appointmentpack.contact;

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
public class HealthcareContactController {

    private final HealthcareContactService healthcareContactService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @PostMapping("/patient-records/{patientRecordId}/healthcare-contacts")
    public ResponseEntity<HealthcareContactResponse> createHealthcareContact(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId,
            @Valid @RequestBody CreateHealthcareContactRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        HealthcareContactResponse response = healthcareContactService.createHealthcareContact(
                authenticatedUserId,
                patientRecordId,
                request);

        return ResponseEntity.created(URI.create("/api/v1/healthcare-contacts/" + response.id())).body(response);
    }

    @GetMapping("/patient-records/{patientRecordId}/healthcare-contacts")
    public ResponseEntity<List<HealthcareContactResponse>> getHealthcareContacts(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(healthcareContactService.getHealthcareContacts(authenticatedUserId, patientRecordId));
    }

    @GetMapping("/healthcare-contacts/{healthcareContactId}")
    public ResponseEntity<HealthcareContactResponse> getHealthcareContact(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID healthcareContactId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(healthcareContactService.getHealthcareContact(
                authenticatedUserId,
                healthcareContactId));
    }

    @PutMapping("/healthcare-contacts/{healthcareContactId}")
    public ResponseEntity<HealthcareContactResponse> updateHealthcareContact(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID healthcareContactId,
            @Valid @RequestBody UpdateHealthcareContactRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(healthcareContactService.updateHealthcareContact(
                authenticatedUserId,
                healthcareContactId,
                request));
    }

    @PatchMapping("/healthcare-contacts/{healthcareContactId}/archive")
    public ResponseEntity<HealthcareContactResponse> archiveHealthcareContact(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID healthcareContactId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(healthcareContactService.archiveHealthcareContact(
                authenticatedUserId,
                healthcareContactId));
    }
}