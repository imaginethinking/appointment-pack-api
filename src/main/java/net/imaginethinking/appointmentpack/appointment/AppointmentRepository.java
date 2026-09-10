package net.imaginethinking.appointmentpack.appointment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Defines the database queries used for appointments.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    /**
     * Checks whether a matching appointment already exists.
     */
    boolean existsBySourceDocument_Id(UUID sourceDocumentId);

    /**
     * Loads the matching appointments with the earliest appointment first.
     */
    List<Appointment> findAllByPatientRecord_IdAndArchivedAtIsNullOrderByDateAscStartTimeAsc(UUID patientRecordId);
}