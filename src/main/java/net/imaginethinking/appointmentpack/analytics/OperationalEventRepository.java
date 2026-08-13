package net.imaginethinking.appointmentpack.analytics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OperationalEventRepository extends JpaRepository<OperationalEvent, UUID> {

    boolean existsBySourceEventId(UUID sourceEventId);
}