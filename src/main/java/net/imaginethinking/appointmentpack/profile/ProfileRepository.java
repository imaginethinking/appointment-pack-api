package net.imaginethinking.appointmentpack.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
/**
 * Defines the database queries used for profiles.
 */
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    /**
     * Loads the matching profile when it exists.
     */
    Optional<Profile> findByUserId(UUID userId);

}
