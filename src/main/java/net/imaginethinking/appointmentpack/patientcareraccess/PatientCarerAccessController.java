package net.imaginethinking.appointmentpack.patientcareraccess;

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
@RequestMapping("/api/v1/patient-carer-access")
public class PatientCarerAccessController {

    private final PatientCarerAccessService patientCarerAccessService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @PostMapping
    public ResponseEntity<PatientCarerAccessResponse> createInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCarerInvitationRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        PatientCarerAccessResponse response = patientCarerAccessService.createInvitation(authenticatedUserId, request);

        return ResponseEntity.created(URI.create("/api/v1/patient-carer-access/" + response.id())).body(response);
    }

    @GetMapping("/{accessId}")
    public ResponseEntity<PatientCarerAccessResponse> getRelationship(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID accessId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientCarerAccessService.getRelationship(authenticatedUserId, accessId));
    }

    @GetMapping("/as-patient")
    public ResponseEntity<List<PatientCarerAccessResponse>> getRelationshipsAsPatient(
            @AuthenticationPrincipal Jwt jwt) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientCarerAccessService.getRelationshipsAsPatient(authenticatedUserId));
    }

    @GetMapping("/as-carer")
    public ResponseEntity<List<PatientCarerAccessResponse>> getRelationshipsAsCarer(
            @AuthenticationPrincipal Jwt jwt) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientCarerAccessService.getRelationshipsAsCarer(authenticatedUserId));
    }

    @PatchMapping("/{accessId}/accept")
    public ResponseEntity<PatientCarerAccessResponse> acceptInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID accessId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientCarerAccessService.acceptInvitation(authenticatedUserId, accessId));
    }

    @PatchMapping("/{accessId}/decline")
    public ResponseEntity<PatientCarerAccessResponse> declineInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID accessId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientCarerAccessService.declineInvitation(authenticatedUserId, accessId));
    }

    @PatchMapping("/{accessId}/revoke")
    public ResponseEntity<PatientCarerAccessResponse> revokeAccess(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID accessId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientCarerAccessService.revokeAccess(authenticatedUserId, accessId));
    }

    @PatchMapping("/{accessId}/cancel")
    public ResponseEntity<PatientCarerAccessResponse> cancelInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID accessId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientCarerAccessService.cancelInvitation(authenticatedUserId, accessId));
    }

    @PutMapping("/{accessId}/permissions")
    public ResponseEntity<PatientCarerAccessResponse> updatePermissions(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID accessId,
            @Valid @RequestBody UpdatePatientCarerPermissionsRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientCarerAccessService.updatePermissions(authenticatedUserId, accessId, request));
    }
}