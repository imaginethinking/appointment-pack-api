package net.imaginethinking.appointmentpack.bloodtest;

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
 * Handles requests for creating and managing blood tests and their results.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class BloodTestController {

    private final BloodTestService bloodTestService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    /**
     * Creates a new blood test using the submitted details.
     */
    @PostMapping("/patient-records/{patientRecordId}/blood-tests")
    public ResponseEntity<BloodTestResponse> createBloodTest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId,
            @Valid @RequestBody CreateBloodTestRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        BloodTestResponse response = bloodTestService.createBloodTest(authenticatedUserId, patientRecordId, request);

        return ResponseEntity.created(URI.create("/api/v1/blood-tests/" + response.id())).body(response);
    }

    /**
     * Returns the requested blood tests for the signed in user.
     */
    @GetMapping("/patient-records/{patientRecordId}/blood-tests")
    public ResponseEntity<List<BloodTestResponse>> getBloodTests(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(bloodTestService.getBloodTests(authenticatedUserId, patientRecordId));
    }

    /**
     * Returns the requested blood test for the signed in user.
     */
    @GetMapping("/blood-tests/{bloodTestId}")
    public ResponseEntity<BloodTestResponse> getBloodTest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID bloodTestId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(bloodTestService.getBloodTest(authenticatedUserId, bloodTestId));
    }

    /**
     * Saves the submitted changes to the requested blood test.
     */
    @PutMapping("/blood-tests/{bloodTestId}")
    public ResponseEntity<BloodTestResponse> updateBloodTest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID bloodTestId,
            @Valid @RequestBody UpdateBloodTestRequest request) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(bloodTestService.updateBloodTest(authenticatedUserId, bloodTestId, request));
    }

    /**
     * Archives the requested blood test.
     */
    @PatchMapping("/blood-tests/{bloodTestId}/archive")
    public ResponseEntity<BloodTestResponse> archiveBloodTest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID bloodTestId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(bloodTestService.archiveBloodTest(authenticatedUserId, bloodTestId));
    }
}