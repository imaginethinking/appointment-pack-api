package net.imaginethinking.appointmentpack.appointment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AppointmentRepository
        extends JpaRepository<Appointment, UUID> {

    boolean existsBySourceDocument_Id(UUID sourceDocumentId);

    List<Appointment> findAllByPatientRecord_IdOrderByDateAscStartTimeAsc(UUID patientRecordId);
}