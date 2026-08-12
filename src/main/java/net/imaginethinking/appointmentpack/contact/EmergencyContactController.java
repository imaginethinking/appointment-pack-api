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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class EmergencyContactController {

    private final EmergencyContactService emergencyContactService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @PostMapping("/patient-records/{patientRecordId}/emergency-contacts")
    public ResponseEntity<EmergencyContactResponse> createEmergencyContact(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId,
            @Valid @RequestBody CreateEmergencyContactRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        EmergencyContactResponse response = emergencyContactService.createEmergencyContact(
                authenticatedUserId,
                patientRecordId,
                request);

        return ResponseEntity.created(URI.create("/api/v1/emergency-contacts/" + response.id())).body(response);
    }

    @GetMapping("/patient-records/{patientRecordId}/emergency-contacts")
    public ResponseEntity<List<EmergencyContactResponse>> getEmergencyContacts(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(emergencyContactService.getEmergencyContacts(authenticatedUserId, patientRecordId));
    }

    @GetMapping("/emergency-contacts/{emergencyContactId}")
    public ResponseEntity<EmergencyContactResponse> getEmergencyContact(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID emergencyContactId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(emergencyContactService.getEmergencyContact(authenticatedUserId, emergencyContactId));
    }

    @PutMapping("/emergency-contacts/{emergencyContactId}")
    public ResponseEntity<EmergencyContactResponse> updateEmergencyContact(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID emergencyContactId,
            @Valid @RequestBody UpdateEmergencyContactRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(emergencyContactService.updateEmergencyContact(
                authenticatedUserId,
                emergencyContactId,
                request));
    }

    @PatchMapping("/emergency-contacts/{emergencyContactId}/archive")
    public ResponseEntity<EmergencyContactResponse> archiveEmergencyContact(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID emergencyContactId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(emergencyContactService.archiveEmergencyContact(
                authenticatedUserId,
                emergencyContactId));
    }
}