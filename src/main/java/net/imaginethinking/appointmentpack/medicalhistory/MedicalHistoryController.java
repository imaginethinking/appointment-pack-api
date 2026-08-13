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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MedicalHistoryController {

    private final MedicalHistoryService medicalHistoryService;

    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @PostMapping("/patient-records/{patientRecordId}/medical-history")
    public ResponseEntity<MedicalHistoryEntryResponse> createEntry(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId,
            @Valid @RequestBody MedicalHistoryEntryRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        MedicalHistoryEntryResponse response = medicalHistoryService.createEntry(
                authenticatedUserId,
                patientRecordId,
                request);

        return ResponseEntity.created(URI.create("/api/v1/medical-history/" + response.id())).body(response);
    }

    @GetMapping("/patient-records/{patientRecordId}/medical-history")
    public ResponseEntity<List<MedicalHistoryEntryResponse>> getMedicalHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        List<MedicalHistoryEntryResponse> response = medicalHistoryService.getHistory(
                authenticatedUserId,
                patientRecordId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/medical-history/{entryId}")
    public ResponseEntity<MedicalHistoryEntryResponse> getEntry(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID entryId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        MedicalHistoryEntryResponse response = medicalHistoryService.getEntry(authenticatedUserId, entryId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/medical-history/{entryId}")
    public ResponseEntity<MedicalHistoryEntryResponse> updateEntry(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID entryId,
            @Valid @RequestBody MedicalHistoryEntryRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        MedicalHistoryEntryResponse response = medicalHistoryService.updateEntry(authenticatedUserId, entryId, request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/medical-history/{entryId}/archive")
    public ResponseEntity<MedicalHistoryEntryResponse> archiveEntry(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID entryId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        MedicalHistoryEntryResponse response = medicalHistoryService.archiveEntry(authenticatedUserId, entryId);

        return ResponseEntity.ok(response);
    }
}