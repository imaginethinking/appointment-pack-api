package net.imaginethinking.appointmentpack.medication;

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
 * Handles requests for creating and managing medications.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MedicationController {

    private final MedicationService medicationService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    /**
     * Creates a new medication using the submitted details.
     */
    @PostMapping("/patient-records/{patientRecordId}/medications")
    public ResponseEntity<MedicationResponse> createMedication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId,
            @Valid @RequestBody CreateMedicationRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        MedicationResponse response = medicationService.createMedication(authenticatedUserId, patientRecordId, request);

        return ResponseEntity.created(URI.create("/api/v1/medications/" + response.id())).body(response);
    }

    /**
     * Returns the requested medications for the signed in user.
     */
    @GetMapping("/patient-records/{patientRecordId}/medications")
    public ResponseEntity<List<MedicationResponse>> getMedications(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        List<MedicationResponse> response = medicationService.getMedications(authenticatedUserId, patientRecordId);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns the requested medication for the signed in user.
     */
    @GetMapping("/medications/{medicationId}")
    public ResponseEntity<MedicationResponse> getMedication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID medicationId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        MedicationResponse response = medicationService.getMedication(authenticatedUserId, medicationId);

        return ResponseEntity.ok(response);
    }

    /**
     * Saves the submitted changes to the requested medication.
     */
    @PutMapping("/medications/{medicationId}")
    public ResponseEntity<MedicationResponse> updateMedication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID medicationId,
            @Valid @RequestBody UpdateMedicationRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        MedicationResponse response = medicationService.updateMedication(authenticatedUserId, medicationId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Archives the requested medication.
     */
    @PatchMapping("/medications/{medicationId}/archive")
    public ResponseEntity<MedicationResponse> archiveMedication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID medicationId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        MedicationResponse response = medicationService.archiveMedication(authenticatedUserId, medicationId);

        return ResponseEntity.ok(response);
    }
}