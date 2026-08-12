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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MedicationController {

    private final MedicationService medicationService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @PostMapping("/patient-records/{patientRecordId}/medications")
    public ResponseEntity<MedicationResponse> createMedication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId,
            @Valid @RequestBody CreateMedicationRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        MedicationResponse response = medicationService.createMedication(authenticatedUserId, patientRecordId, request);

        return ResponseEntity.created(URI.create("/api/v1/medications/" + response.id())).body(response);
    }

    @GetMapping("/patient-records/{patientRecordId}/medications")
    public ResponseEntity<List<MedicationResponse>> getMedications(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        List<MedicationResponse> response = medicationService.getMedications(authenticatedUserId, patientRecordId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/medications/{medicationId}")
    public ResponseEntity<MedicationResponse> getMedication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID medicationId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        MedicationResponse response = medicationService.getMedication(authenticatedUserId, medicationId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/medications/{medicationId}")
    public ResponseEntity<MedicationResponse> updateMedication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID medicationId,
            @Valid @RequestBody UpdateMedicationRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        MedicationResponse response = medicationService.updateMedication(authenticatedUserId, medicationId, request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/medications/{medicationId}/archive")
    public ResponseEntity<MedicationResponse> archiveMedication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID medicationId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        MedicationResponse response = medicationService.archiveMedication(authenticatedUserId, medicationId);

        return ResponseEntity.ok(response);
    }
}