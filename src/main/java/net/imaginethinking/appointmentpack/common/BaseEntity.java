package net.imaginethinking.appointmentpack.common;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Provides the shared ID and timestamps used by saved records.
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Sets both timestamps when a record is first saved.
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /**
     * Updates the modified timestamp before an existing record is saved again.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}