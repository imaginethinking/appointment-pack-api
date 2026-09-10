package net.imaginethinking.appointmentpack.user;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines the database queries used for user accounts.
 */
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * Loads the matching user account when it exists.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a matching user account already exists.
     */
    boolean existsByEmail(String email);

    /**
     * Loads the matching user accounts.
     */
    @EntityGraph(attributePaths = "profile")
    List<User> findAllByIdIn(Collection<UUID> userIds);
}
