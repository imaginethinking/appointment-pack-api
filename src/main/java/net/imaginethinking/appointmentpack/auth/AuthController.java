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

/**
 * Handles registration, login, account recovery and MFA requests.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    /**
     * Creates a new account from the submitted registration details.
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
        RegisterResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Checks the submitted login details and returns the next authentication state for the account.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Completes a pending MFA login using the supplied challenge and authenticator code.
     */
    @PostMapping("/login/mfa")
    public ResponseEntity<LoginResponse> completeMfaLogin(@RequestBody @Valid MfaLoginRequest request) {
        LoginResponse response = authService.completeMfaLogin(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Requests another verification email for the submitted account address.
     */
    @PostMapping("/email-verification/resend")
    public ResponseEntity<Void> resendEmailVerification(@RequestBody @Valid EmailVerificationResendRequest request) {
        emailVerificationService.resend(request);

        return ResponseEntity.noContent().build();
    }

    /**
     * Confirms an email address using the supplied verification token.
     */
    @PostMapping("/email-verification/confirm")
    public ResponseEntity<Void> confirmEmailVerification(@RequestBody @Valid EmailVerificationConfirmRequest request) {
        emailVerificationService.confirm(request);

        return ResponseEntity.noContent().build();
    }

    /**
     * Requests a password reset email for the submitted account address.
     */
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody @Valid PasswordResetRequest request) {
        passwordResetService.requestReset(request);

        return ResponseEntity.noContent().build();
    }

    /**
     * Confirms a password reset using the supplied token and new password.
     */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@RequestBody @Valid PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request);

        return ResponseEntity.noContent().build();
    }

    /**
     * Returns the current MFA status for the signed in account.
     */
    @GetMapping("/security")
    public ResponseEntity<AccountSecurityResponse> getAccountSecurity(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        return ResponseEntity.ok(authService.getAccountSecurity(userId));
    }

    /**
     * Changes the password for the signed in account after checking the current password.
     */
    @PostMapping("/password/change")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid PasswordChangeRequest request
    ) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);
        passwordResetService.changePassword(userId, request);

        return ResponseEntity.noContent().build();
    }

    /**
     * Starts MFA setup for the signed in account and returns the provisioning details.
     */
    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);
        MfaSetupResponse response = authService.setupMfa(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Confirms the pending MFA setup using the authenticator code entered by the user.
     */
    @PostMapping("/mfa/confirm")
    public ResponseEntity<Void> confirmMfa(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid MfaConfirmRequest request
    ) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);
        authService.confirmMfa(userId, request);

        return ResponseEntity.noContent().build();
    }

    /**
     * Disables MFA after checking the authenticator code for the signed in account.
     */
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