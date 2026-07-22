package net.imaginethinking.appointmentpack.profile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getCurrentProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);
        ProfileResponse response = profileService.getCurrentProfile(userId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateCurrentProfile(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid UpdateProfileRequest request) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);
        ProfileResponse response = profileService.updateCurrentProfile(userId, request);

        return ResponseEntity.ok(response);
    }

}
