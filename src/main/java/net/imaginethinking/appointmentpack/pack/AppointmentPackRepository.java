package net.imaginethinking.appointmentpack.pack;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Defines the database queries used for Appointment Packs.
 */
public interface AppointmentPackRepository
        extends JpaRepository<AppointmentPack, UUID> {

    /**
     * Loads the matching Appointment Packs.
     */
    List<AppointmentPack> findAllByPatientRecord_IdAndArchivedAtIsNullOrderByGeneratedAtDesc(UUID patientRecordId);
}
