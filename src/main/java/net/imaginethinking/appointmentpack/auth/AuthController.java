package net.imaginethinking.appointmentpack.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
        RegisterResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/mfa")
    public ResponseEntity<LoginResponse> completeMfaLogin(@RequestBody @Valid MfaLoginRequest request) {
        LoginResponse response = authService.completeMfaLogin(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);
        MfaSetupResponse response = authService.setupMfa(userId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/confirm")
    public ResponseEntity<Void> confirmMfa(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid MfaConfirmRequest request) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);
        authService.confirmMfa(userId, request);

        return ResponseEntity.noContent().build();
    }
}
