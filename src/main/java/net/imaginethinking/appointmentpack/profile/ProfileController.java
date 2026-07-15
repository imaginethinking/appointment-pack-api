package net.imaginethinking.appointmentpack.profile;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        ProfileResponse profile = profileService.getCurrentProfile(userId);

        return ResponseEntity.ok(profile);
    }

}
