package net.imaginethinking.appointmentpack.profile;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.address.AddressMapper;
import net.imaginethinking.appointmentpack.common.TextNormalizer;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Loads and updates the profile linked to the current account.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final PatientRecordRepository patientRecordRepository;
    private final AppEventPublisher appEventPublisher;

    /**
     * Loads the profile for the signed in user and returns not found when no profile exists.
     */
    @Transactional(readOnly = true)
    public ProfileResponse getCurrentProfile(UUID userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));

        return ProfileResponse.from(profile);
    }

    /**
     * Loads the current profile, applies the submitted values and records the change when a patient record exists.
     */
    @Transactional
    public ProfileResponse updateCurrentProfile(UUID userId, UpdateProfileRequest request) {
        Profile profile = findByUserId(userId);

        profile.setFirstName(TextNormalizer.strip(request.firstName()));
        profile.setLastName(TextNormalizer.strip(request.lastName()));
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setGender(TextNormalizer.stripToNull(request.gender()));
        profile.setAddress(AddressMapper.toAddress(request.address()));

        patientRecordRepository.findByProfileUserId(userId)
                .ifPresent(patientRecord -> appEventPublisher.publish(
                        PatientActivityEvent.create(
                                userId,
                                patientRecord.getId(),
                                PatientResourceType.PROFILE,
                                profile.getId(),
                                PatientActivityAction.UPDATED)));

        return ProfileResponse.from(profile);
    }

    /**
     * Loads the profile linked to the user or returns not found when it does not exist.
     */
    private Profile findByUserId(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
    }
}