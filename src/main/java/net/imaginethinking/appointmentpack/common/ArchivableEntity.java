package net.imaginethinking.appointmentpack.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.Instant;

/**
 * Adds archive information to records that stay stored after they are removed from active use.
 */
@Getter
@MappedSuperclass
public abstract class ArchivableEntity extends BaseEntity {

    @Column(name = "archived_at")
    private Instant archivedAt;

    /**
     * Checks whether the record has already been archived.
     */
    public boolean isArchived() {
        return archivedAt != null;
    }

    /**
     * Sets the archive time once and leaves an already archived record unchanged.
     */
    public void archive() {
        if (archivedAt == null) {
            archivedAt = Instant.now();
        }
    }
}