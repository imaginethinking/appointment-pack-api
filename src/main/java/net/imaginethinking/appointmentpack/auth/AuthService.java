package net.imaginethinking.appointmentpack.auth;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.profile.Profile;
import net.imaginethinking.appointmentpack.security.MfaTotpService;
import net.imaginethinking.appointmentpack.user.User;
import net.imaginethinking.appointmentpack.user.UserRepository;
import net.imaginethinking.appointmentpack.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final MfaTotpService mfaTotpService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email address already registered");
        }

        if (!Objects.equals(request.password(), request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.PATIENT);

        Profile profile = new Profile();
        profile.setUser(user);
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setDateOfBirth(request.dateOfBirth());

        user.setProfile(profile);

        User registeredUser = userRepository.save(user);

        return new RegisterResponse(
                registeredUser.getId(),
                registeredUser.getEmail(),
                registeredUser.getProfile().getId()
        );
    }

    public AuthResponse login(LoginRequest request) {
        return null;
    }

    public MfaSetupResponse setupMfa(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String secret = mfaTotpService.generateSecret();
        String qrCodeUri = mfaTotpService.generateQrCodeUri(email, secret);

        user.setMfaSecret(secret);
        user.setMfaEnabled(false);

        userRepository.save(user);

        return new MfaSetupResponse(secret, qrCodeUri);
    }

}
