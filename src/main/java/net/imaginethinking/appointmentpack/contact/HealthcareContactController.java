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

/**
 * Handles requests for creating and managing healthcare contacts.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class HealthcareContactController {

    private final HealthcareContactService healthcareContactService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    /**
     * Creates a new healthcare contact using the submitted details.
     */
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

    /**
     * Returns the requested healthcare contacts for the signed in user.
     */
    @GetMapping("/patient-records/{patientRecordId}/healthcare-contacts")
    public ResponseEntity<List<HealthcareContactResponse>> getHealthcareContacts(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(healthcareContactService.getHealthcareContacts(authenticatedUserId, patientRecordId));
    }

    /**
     * Returns the requested healthcare contact for the signed in user.
     */
    @GetMapping("/healthcare-contacts/{healthcareContactId}")
    public ResponseEntity<HealthcareContactResponse> getHealthcareContact(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID healthcareContactId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(healthcareContactService.getHealthcareContact(
                authenticatedUserId,
                healthcareContactId));
    }

    /**
     * Saves the submitted changes to the requested healthcare contact.
     */
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

    /**
     * Archives the requested healthcare contact.
     */
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