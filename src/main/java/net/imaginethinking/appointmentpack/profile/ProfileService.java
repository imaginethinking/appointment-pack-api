package net.imaginethinking.appointmentpack.profile;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getCurrentProfile(UUID userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));

        return ProfileResponse.from(profile);
    }

    @Transactional
    public ProfileResponse updateCurrentProfile(UUID userId, UpdateProfileRequest request) {
        Profile profile = findByUserId(userId);

        profile.setFirstName(request.firstName().trim());
        profile.setLastName(request.lastName().trim());
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setGender(normaliseOptionalValue(request.gender()));

        return ProfileResponse.from(profile);
    }

    private Profile findByUserId(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Profile not found"
                ));
    }

    private String normaliseOptionalValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
