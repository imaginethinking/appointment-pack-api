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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/patient-records")
public class PatientRecordController {

    private final PatientRecordService patientRecordService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @PostMapping
    public ResponseEntity<PatientRecordResponse> createCurrentPatientRecord(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePatientRecordRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        PatientRecordResponse response = patientRecordService.createCurrentPatientRecord(authenticatedUserId, request);

        return ResponseEntity.created(URI.create("/api/v1/patient-records/" + response.id())).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<PatientRecordResponse> getCurrentPatientRecord(
            @AuthenticationPrincipal Jwt jwt) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientRecordService.getCurrentPatientRecord(authenticatedUserId));
    }

    @GetMapping("/{patientRecordId}")
    public ResponseEntity<PatientRecordResponse> getPatientRecord(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientRecordService.getPatientRecord(authenticatedUserId, patientRecordId));
    }

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