package net.imaginethinking.appointmentpack.profile;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.user.CreateUserRequest;
import net.imaginethinking.appointmentpack.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository profileRepository;

    @Transactional
    public void createProfile(CreateUserRequest request, User user) {

        Profile profile = new Profile();
        profile.setUser(user);
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setDateOfBirth(request.dateOfBirth());

        profileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(UUID id) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));

        return ProfileResponse.from(profile);
    }

}
