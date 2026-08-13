package net.imaginethinking.appointmentpack.document.processing.review;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.address.AddressMapper;
import net.imaginethinking.appointmentpack.appointment.*;
import net.imaginethinking.appointmentpack.common.TextNormalizer;
import net.imaginethinking.appointmentpack.document.Document;
import net.imaginethinking.appointmentpack.document.DocumentPermission;
import net.imaginethinking.appointmentpack.document.DocumentStatus;
import net.imaginethinking.appointmentpack.document.DocumentType;
import net.imaginethinking.appointmentpack.document.processing.DocumentProcessingRecordService;
import net.imaginethinking.appointmentpack.document.processing.DocumentProcessingResult;
import net.imaginethinking.appointmentpack.document.processing.DocumentProcessingResultMapper;
import net.imaginethinking.appointmentpack.document.processing.api.DocumentProcessingResultResponse;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import net.imaginethinking.appointmentpack.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentDocumentReviewService {

    private final DocumentProcessingRecordService processingRecordService;
    private final AppointmentRepository appointmentRepository;
    private final PatientRecordAccessService patientRecordAccessService;
    private final DocumentProcessingResultMapper processingResultMapper;
    private final EntityManager entityManager;

    @Transactional
    public AppointmentResponse confirmAppointment(
            UUID authenticatedUserId,
            UUID documentId,
            AppointmentConfirmationRequest request) {
        Document document = processingRecordService.requireAvailableDocument(documentId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.EDIT);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                document.getPatientRecord(),
                AppointmentPermission.EDIT);

        validateAppointmentCanBeConfirmed(document);

        if (appointmentRepository.existsBySourceDocument_Id(documentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An appointment already exists for this document");
        }

        validateAppointmentTimes(request);

        DocumentProcessingResult result = processingRecordService.requireProcessingResult(documentId);

        User reviewingUser = entityManager.getReference(User.class, authenticatedUserId);

        result.setAppointmentReviewedBy(reviewingUser);
        result.setAppointmentReviewedAt(Instant.now());

        Appointment appointment = new Appointment();

        appointment.setPatientRecord(document.getPatientRecord());
        appointment.setDate(request.date());
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(request.endTime());
        appointment.setService(TextNormalizer.stripToNull(request.service()));
        appointment.setAppointmentType(TextNormalizer.stripToNull(request.appointmentType()));
        appointment.setClinicianOrTeam(TextNormalizer.stripToNull(request.clinicianOrTeam()));
        appointment.setLocationName(TextNormalizer.stripToNull(request.locationName()));
        appointment.setAddress(AddressMapper.toAddress(request.address()));
        appointment.setNotes(TextNormalizer.stripToNull(request.notes()));
        appointment.setSourceDocument(document);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        document.setStatus(DocumentStatus.ACCEPTED);
        document.setProcessingFailureReason(null);

        return AppointmentResponse.from(savedAppointment);
    }

    @Transactional
    public DocumentProcessingResultResponse rejectAppointment(UUID authenticatedUserId, UUID documentId) {
        Document document = processingRecordService.requireAvailableDocument(documentId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.EDIT);

        if (document.getDocumentType() != DocumentType.APPOINTMENT_LETTER || document.getStatus() != DocumentStatus.READY_FOR_APPOINTMENT_REVIEW) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Appointment details cannot be rejected in the current document state");
        }

        DocumentProcessingResult result = processingRecordService.requireProcessingResult(documentId);

        result.setAppointmentReviewedBy(entityManager.getReference(User.class, authenticatedUserId));
        result.setAppointmentReviewedAt(Instant.now());

        document.setStatus(DocumentStatus.REJECTED);
        document.setProcessingFailureReason(null);

        return processingResultMapper.toResponse(document, result);
    }

    private void validateAppointmentCanBeConfirmed(Document document) {
        if (document.getDocumentType() != DocumentType.APPOINTMENT_LETTER || document.getStatus() != DocumentStatus.READY_FOR_APPOINTMENT_REVIEW) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Appointment cannot be confirmed in the current document state");
        }
    }

    private void validateAppointmentTimes(AppointmentConfirmationRequest request) {
        if (request.endTime() != null && !request.endTime().isAfter(request.startTime())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Appointment end time must be after the start time");
        }
    }

}