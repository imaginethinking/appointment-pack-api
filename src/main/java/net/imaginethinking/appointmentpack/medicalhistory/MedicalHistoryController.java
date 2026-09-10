package net.imaginethinking.appointmentpack.medicalhistory;

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
 * Handles requests for creating and managing Medical History entries.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MedicalHistoryController {

    private final MedicalHistoryService medicalHistoryService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    /**
     * Creates a new Medical History entry using the submitted details.
     */
    @PostMapping("/patient-records/{patientRecordId}/medical-history")
    public ResponseEntity<MedicalHistoryEntryResponse> createMedicalHistoryEntry(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId,
            @Valid @RequestBody CreateMedicalHistoryEntryRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        MedicalHistoryEntryResponse response = medicalHistoryService.createMedicalHistoryEntry(
                authenticatedUserId,
                patientRecordId,
                request);

        return ResponseEntity.created(URI.create("/api/v1/medical-history/" + response.id())).body(response);
    }

    /**
     * Returns the requested Medical History entries for the signed in user.
     */
    @GetMapping("/patient-records/{patientRecordId}/medical-history")
    public ResponseEntity<List<MedicalHistoryEntryResponse>> getMedicalHistoryEntries(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(medicalHistoryService.getMedicalHistoryEntries(authenticatedUserId, patientRecordId));
    }

    /**
     * Returns the requested Medical History entry for the signed in user.
     */
    @GetMapping("/medical-history/{entryId}")
    public ResponseEntity<MedicalHistoryEntryResponse> getMedicalHistoryEntry(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID entryId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(medicalHistoryService.getMedicalHistoryEntry(authenticatedUserId, entryId));
    }

    /**
     * Saves the submitted changes to the requested Medical History entry.
     */
    @PutMapping("/medical-history/{entryId}")
    public ResponseEntity<MedicalHistoryEntryResponse> updateMedicalHistoryEntry(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID entryId,
            @Valid @RequestBody UpdateMedicalHistoryEntryRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(medicalHistoryService.updateMedicalHistoryEntry(
                authenticatedUserId,
                entryId,
                request));
    }

    /**
     * Archives the requested Medical History entry.
     */
    @PatchMapping("/medical-history/{entryId}/archive")
    public ResponseEntity<MedicalHistoryEntryResponse> archiveMedicalHistoryEntry(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID entryId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(medicalHistoryService.archiveMedicalHistoryEntry(authenticatedUserId, entryId));
    }
}