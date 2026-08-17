package net.imaginethinking.appointmentpack.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.auth.mfa.MfaConfirmRequest;
import net.imaginethinking.appointmentpack.auth.mfa.MfaLoginRequest;
import net.imaginethinking.appointmentpack.auth.mfa.MfaSetupResponse;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
        RegisterResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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

    @PostMapping("/email-verification/resend")
    public ResponseEntity<Void> resendEmailVerification(@RequestBody @Valid EmailVerificationResendRequest request) {
        emailVerificationService.resend(request);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email-verification/confirm")
    public ResponseEntity<Void> confirmEmailVerification(@RequestBody @Valid EmailVerificationConfirmRequest request) {
        emailVerificationService.confirm(request);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody @Valid PasswordResetRequest request) {
        passwordResetService.requestReset(request);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@RequestBody @Valid PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/security")
    public ResponseEntity<AccountSecurityResponse> getAccountSecurity(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(authService.getAccountSecurity(userId));
    }

    @PostMapping("/password/change")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid PasswordChangeRequest request
    ) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);
        passwordResetService.changePassword(userId, request);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);
        MfaSetupResponse response = authService.setupMfa(userId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/confirm")
    public ResponseEntity<Void> confirmMfa(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid MfaConfirmRequest request
    ) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);
        authService.confirmMfa(userId, request);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<Void> disableMfa(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid MfaConfirmRequest request
    ) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);
        authService.disableMfa(userId, request);

        return ResponseEntity.noContent().build();
    }
}