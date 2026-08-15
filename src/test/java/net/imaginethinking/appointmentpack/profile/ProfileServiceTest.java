package net.imaginethinking.appointmentpack.profile;

import net.imaginethinking.appointmentpack.address.AddressRequest;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordRepository;
import net.imaginethinking.appointmentpack.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private PatientRecordRepository patientRecordRepository;

    @Mock
    private AppEventPublisher appEventPublisher;

    private ProfileService service;

    @BeforeEach
    void setUp() {
        service = new ProfileService(profileRepository, patientRecordRepository, appEventPublisher);
    }

    @Test
    void shouldReturnCurrentProfile() {
        UUID userId = UUID.randomUUID();
        Profile profile = profile(userId);

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        ProfileResponse response = service.getCurrentProfile(userId);

        assertEquals(profile.getId(), response.id());
        assertEquals(userId, response.userId());
        assertEquals("Patient", response.firstName());
        assertEquals("User", response.lastName());
    }

    @Test
    void shouldUpdateProfileAndPublishPatientActivityWhenRecordExists() {
        UUID userId = UUID.randomUUID();
        Profile profile = profile(userId);

        PatientRecord patientRecord = new PatientRecord();

        ReflectionTestUtils.setField(patientRecord, "id", UUID.randomUUID());

        patientRecord.setProfile(profile);

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        when(patientRecordRepository.findByProfileUserId(userId)).thenReturn(Optional.of(patientRecord));

        UpdateProfileRequest request = new UpdateProfileRequest(
                " Updated ",
                " Name ",
                LocalDate.of(1991, 2, 3),
                " Female ",
                new AddressRequest(" 1 Test Street ", " ", " Glasgow ", " ", " G1 1AA ", " Scotland "));

        ProfileResponse response = service.updateCurrentProfile(userId, request);

        assertEquals("Updated", profile.getFirstName());
        assertEquals("Name", profile.getLastName());
        assertEquals("Female", profile.getGender());
        assertEquals("1 Test Street", response.address().addressLine1());
        assertNull(response.address().addressLine2());

        verify(appEventPublisher).publish(any());
    }

    @Test
    void shouldNotPublishPatientActivityBeforePatientRecordExists() {
        UUID userId = UUID.randomUUID();
        Profile profile = profile(userId);

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        when(patientRecordRepository.findByProfileUserId(userId)).thenReturn(Optional.empty());

        service.updateCurrentProfile(
                userId,
                new UpdateProfileRequest("Patient", "User", LocalDate.of(1990, 1, 1), null, null));

        verify(appEventPublisher, never()).publish(any());
    }

    @Test
    void shouldReturnNotFoundWhenProfileDoesNotExist() {
        UUID userId = UUID.randomUUID();

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getCurrentProfile(userId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private Profile profile(UUID userId) {
        User user = new User();

        ReflectionTestUtils.setField(user, "id", userId);

        Profile profile = new Profile();

        ReflectionTestUtils.setField(profile, "id", UUID.randomUUID());

        profile.setUser(user);
        profile.setFirstName("Patient");
        profile.setLastName("User");
        profile.setDateOfBirth(LocalDate.of(1990, 1, 1));

        user.setProfile(profile);

        return profile;
    }
}