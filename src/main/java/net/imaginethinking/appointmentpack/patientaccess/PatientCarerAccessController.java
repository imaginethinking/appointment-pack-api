package net.imaginethinking.appointmentpack.patientaccess;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patient-carer-access")
@RequiredArgsConstructor
public class PatientCarerAccessController {
    private final PatientCarerAccessService patientCarerAccessService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @PostMapping("/invitation")
    public ResponseEntity<PatientCarerAccessResponse> inviteCarer(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid CreateCarerInvitationRequest request) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        PatientCarerAccessResponse response = patientCarerAccessService.inviteCarer(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/as-patient")
    public ResponseEntity<List<PatientCarerAccessResponse>> getAsPatient(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        List<PatientCarerAccessResponse> response = patientCarerAccessService.getRelationshipsAsPatient(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/as-carer")
    public ResponseEntity<List<PatientCarerAccessResponse>> getAsCarer(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        List<PatientCarerAccessResponse> response = patientCarerAccessService.getRelationshipsAsCarer(userId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{accessId}/accept")
    public ResponseEntity<PatientCarerAccessResponse> acceptInvitation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID accessId) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        PatientCarerAccessResponse response = patientCarerAccessService.acceptInvitation(userId, accessId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{accessId}/decline")
    public ResponseEntity<PatientCarerAccessResponse> declineInvitation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID accessId) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        PatientCarerAccessResponse response = patientCarerAccessService.declineInvitation(userId, accessId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{accessId}/revoke")
    public ResponseEntity<PatientCarerAccessResponse> revokeAccess(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID accessId) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        PatientCarerAccessResponse response = patientCarerAccessService.revokeAccess(userId, accessId);

        return ResponseEntity.ok(response);
    }
}
