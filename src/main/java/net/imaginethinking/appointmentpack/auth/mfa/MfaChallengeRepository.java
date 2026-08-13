package net.imaginethinking.appointmentpack.auth.mfa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MfaChallengeRepository extends JpaRepository<MfaChallenge, UUID> {
}
