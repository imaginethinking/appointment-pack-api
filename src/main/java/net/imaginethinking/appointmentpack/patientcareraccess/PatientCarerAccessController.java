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

/**
 * Handles carer invitations, relationship changes and patient scoped permission updates.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/patient-carer-access")
public class PatientCarerAccessController {

    private final PatientCarerAccessService patientCarerAccessService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    /**
     * Creates a carer invitation for the current user's patient record using the submitted email and permissions.
     */
    @PostMapping
    public ResponseEntity<PatientCarerAccessResponse> createInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCarerInvitationRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        PatientCarerAccessResponse response = patientCarerAccessService.createInvitation(authenticatedUserId, request);

        return ResponseEntity.created(URI.create("/api/v1/patient-carer-access/" + response.id())).body(response);
    }

    /**
     * Returns a patient and carer relationship when the signed in user is one of its participants.
     */
    @GetMapping("/{accessId}")
    public ResponseEntity<PatientCarerAccessResponse> getRelationship(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID accessId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientCarerAccessService.getRelationship(authenticatedUserId, accessId));
    }

    /**
     * Returns the carer relationship history for the current user's patient record.
     */
    @GetMapping("/as-patient")
    public ResponseEntity<List<PatientCarerAccessResponse>> getRelationshipsAsPatient(
            @AuthenticationPrincipal Jwt jwt) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientCarerAccessService.getRelationshipsAsPatient(authenticatedUserId));
    }

    /**
     * Returns the patient relationships where the current user is the invited carer.
     */
    @GetMapping("/as-carer")
    public ResponseEntity<List<PatientCarerAccessResponse>> getRelationshipsAsCarer(
            @AuthenticationPrincipal Jwt jwt) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientCarerAccessService.getRelationshipsAsCarer(authenticatedUserId));
    }

    /**
     * Accepts the requested pending carer invitation for the current user.
     */
    @PatchMapping("/{accessId}/accept")
    public ResponseEntity<PatientCarerAccessResponse> acceptInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID accessId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientCarerAccessService.acceptInvitation(authenticatedUserId, accessId));
    }

    /**
     * Declines the requested pending carer invitation for the current user.
     */
    @PatchMapping("/{accessId}/decline")
    public ResponseEntity<PatientCarerAccessResponse> declineInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID accessId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientCarerAccessService.declineInvitation(authenticatedUserId, accessId));
    }

    /**
     * Revokes an active carer relationship owned by the current user's patient record.
     */
    @PatchMapping("/{accessId}/revoke")
    public ResponseEntity<PatientCarerAccessResponse> revokeAccess(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID accessId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientCarerAccessService.revokeAccess(authenticatedUserId, accessId));
    }

    /**
     * Cancels a pending carer invitation owned by the current user's patient record.
     */
    @PatchMapping("/{accessId}/cancel")
    public ResponseEntity<PatientCarerAccessResponse> cancelInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID accessId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientCarerAccessService.cancelInvitation(authenticatedUserId, accessId));
    }

    /**
     * Replaces the permissions on a patient and carer relationship owned by the current user.
     */
    @PutMapping("/{accessId}/permissions")
    public ResponseEntity<PatientCarerAccessResponse> updatePermissions(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID accessId,
            @Valid @RequestBody UpdatePatientCarerPermissionsRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(patientCarerAccessService.updatePermissions(authenticatedUserId, accessId, request));
    }
}