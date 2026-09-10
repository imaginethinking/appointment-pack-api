package net.imaginethinking.appointmentpack.profile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Handles requests for viewing and updating the current profile.
 */
@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    /**
     * Returns the requested current profile for the signed in user.
     */
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getCurrentProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);
        ProfileResponse response = profileService.getCurrentProfile(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Saves the submitted changes to the requested current profile.
     */
    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateCurrentProfile(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid UpdateProfileRequest request) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);
        ProfileResponse response = profileService.updateCurrentProfile(userId, request);

        return ResponseEntity.ok(response);
    }

}
