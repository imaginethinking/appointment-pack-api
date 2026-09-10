package net.imaginethinking.appointmentpack.patientrecord;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

/**
 * Handles requests for creating, viewing and updating patient records.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/patient-records")
public class PatientRecordController {

    private final PatientRecordService patientRecordService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    /**
     * Creates a new current patient record using the submitted details.
     */
    @PostMapping
    public ResponseEntity<PatientRecordResponse> createCurrentPatientRecord(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePatientRecordRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        PatientRecordResponse response = patientRecordService.createCurrentPatientRecord(authenticatedUserId, request);

        return ResponseEntity.created(URI.create("/api/v1/patient-records/" + response.id())).body(response);
    }

    /**
     * Returns the requested current patient record for the signed in user.
     */
    @GetMapping("/me")
    public ResponseEntity<PatientRecordResponse> getCurrentPatientRecord(
            @AuthenticationPrincipal Jwt jwt) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientRecordService.getCurrentPatientRecord(authenticatedUserId));
    }

    /**
     * Returns the requested patient record for the signed in user.
     */
    @GetMapping("/{patientRecordId}")
    public ResponseEntity<PatientRecordResponse> getPatientRecord(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientRecordService.getPatientRecord(authenticatedUserId, patientRecordId));
    }

    /**
     * Saves the submitted changes to the requested patient record.
     */
    @PutMapping("/{patientRecordId}")
    public ResponseEntity<PatientRecordResponse> updatePatientRecord(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId,
            @Valid @RequestBody UpdatePatientRecordRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientRecordService.updatePatientRecord(
                authenticatedUserId,
                patientRecordId,
                request));
    }
}