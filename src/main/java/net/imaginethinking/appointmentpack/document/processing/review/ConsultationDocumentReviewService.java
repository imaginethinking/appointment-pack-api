package net.imaginethinking.appointmentpack.document.processing.review;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.common.TextNormalizer;
import net.imaginethinking.appointmentpack.document.Document;
import net.imaginethinking.appointmentpack.document.DocumentPermission;
import net.imaginethinking.appointmentpack.document.DocumentStatus;
import net.imaginethinking.appointmentpack.document.DocumentType;
import net.imaginethinking.appointmentpack.document.processing.DocumentProcessingRecordService;
import net.imaginethinking.appointmentpack.document.processing.DocumentProcessingResult;
import net.imaginethinking.appointmentpack.document.processing.DocumentProcessingResultMapper;
import net.imaginethinking.appointmentpack.document.processing.SummarySource;
import net.imaginethinking.appointmentpack.document.processing.api.DocumentProcessingResultResponse;
import net.imaginethinking.appointmentpack.document.processing.api.DocumentSummaryAcceptanceRequest;
import net.imaginethinking.appointmentpack.document.processing.client.DocumentProcessingException;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryEntry;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryEntryRepository;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryPermission;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistorySourceType;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import net.imaginethinking.appointmentpack.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

/**
 * Handles the final review of consultation summaries before they are accepted into Medical History or rejected.
 */
@Service
@RequiredArgsConstructor
public class ConsultationDocumentReviewService {

    private final DocumentProcessingRecordService processingRecordService;
    private final MedicalHistoryEntryRepository medicalHistoryEntryRepository;
    private final PatientRecordAccessService patientRecordAccessService;
    private final DocumentProcessingResultMapper processingResultMapper;
    private final EntityManager entityManager;
    private final AppEventPublisher appEventPublisher;

    /**
     * Checks document and Medical History edit access, validates the reviewed summary and saves it as a Medical
     * History entry.
     */
    @Transactional
    public DocumentProcessingResultResponse acceptSummary(
            UUID authenticatedUserId,
            UUID documentId,
            DocumentSummaryAcceptanceRequest request) {
        Document document = processingRecordService.requireAvailableDocument(documentId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.EDIT);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                document.getPatientRecord(),
                MedicalHistoryPermission.EDIT);

        validateSummaryCanBeAccepted(document);

        if (medicalHistoryEntryRepository.existsBySourceDocumentId(documentId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A medical-history entry already exists for this document");
        }

        DocumentProcessingResult result = processingRecordService.requireProcessingResult(documentId);

        // A failed generated summary can still be replaced with reviewed manual text before it is added
        // to Medical History.
        if (document.getStatus() == DocumentStatus.READY_FOR_SUMMARY_REVIEW) {
            validateGeneratedSummary(result);
        } else {
            applyManualSummary(result);
        }

        String reviewedSummary = TextNormalizer.strip(request.reviewedSummary());

        User reviewingUser = entityManager.getReference(User.class, authenticatedUserId);

        result.setReviewedSummary(reviewedSummary);
        result.setSummaryReviewedBy(reviewingUser);
        result.setSummaryReviewedAt(Instant.now());

        MedicalHistoryEntry historyEntry = new MedicalHistoryEntry();

        historyEntry.setPatientRecord(document.getPatientRecord());
        historyEntry.setTitle(TextNormalizer.strip(request.historyTitle()));
        historyEntry.setSummary(reviewedSummary);
        historyEntry.setEntryDate(request.historyDate());
        historyEntry.setSourceType(MedicalHistorySourceType.DOCUMENT_SUMMARY);
        historyEntry.setSourceDocument(document);
        historyEntry.setCreatedBy(reviewingUser);

        medicalHistoryEntryRepository.save(historyEntry);

        document.setStatus(DocumentStatus.ACCEPTED);
        document.setProcessingFailureReason(null);

        publishDocumentActivity(
                authenticatedUserId,
                document,
                PatientActivityAction.SUMMARY_ACCEPTED);

        return processingResultMapper.toResponse(document, result);
    }

    /**
     * Checks document edit access and marks a consultation summary as rejected when it is waiting for review.
     */
    @Transactional
    public DocumentProcessingResultResponse rejectSummary(
            UUID authenticatedUserId,
            UUID documentId) {
        Document document = processingRecordService.requireAvailableDocument(documentId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.EDIT);

        if (document.getDocumentType() != DocumentType.CONSULTATION_OUTCOME_LETTER || document.getStatus() != DocumentStatus.READY_FOR_SUMMARY_REVIEW) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Document summary cannot be rejected in its current status");
        }

        DocumentProcessingResult result = processingRecordService.requireProcessingResult(documentId);

        result.setReviewedSummary(null);
        result.setSummaryReviewedBy(entityManager.getReference(User.class, authenticatedUserId));
        result.setSummaryReviewedAt(Instant.now());

        document.setStatus(DocumentStatus.REJECTED);
        document.setProcessingFailureReason(null);

        publishDocumentActivity(
                authenticatedUserId,
                document,
                PatientActivityAction.SUMMARY_REJECTED);

        return processingResultMapper.toResponse(document, result);
    }

    /**
     * Publishes the document activity event for the completed change.
     */
    private void publishDocumentActivity(
            UUID authenticatedUserId,
            Document document,
            PatientActivityAction action) {
        appEventPublisher.publish(PatientActivityEvent.create(
                authenticatedUserId,
                document.getPatientRecord().getId(),
                PatientResourceType.DOCUMENT,
                document.getId(),
                action));
    }

    /**
     * Checks that the consultation document is in a state where a reviewed summary can be accepted.
     */
    private void validateSummaryCanBeAccepted(Document document) {
        if (document.getDocumentType() != DocumentType.CONSULTATION_OUTCOME_LETTER) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only consultation outcome summaries can be accepted into medical history");
        }

        boolean acceptable = document.getStatus() == DocumentStatus.READY_FOR_SUMMARY_REVIEW || document.getStatus() == DocumentStatus.SUMMARISATION_FAILED;

        if (!acceptable) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Document summary cannot be accepted in its current status");
        }
    }

    /**
     * Checks that a generated summary and its source information are present before acceptance.
     */
    private void validateGeneratedSummary(DocumentProcessingResult result) {
        if (isBlank(result.getGeneratedSummary()) || result.getSummarySource() == null) {
            throw new DocumentProcessingException("Document processing result contains no generated summary");
        }
    }

    /**
     * Marks the review as a manual summary and clears model details that no longer apply.
     */
    private void applyManualSummary(DocumentProcessingResult result) {
        result.setGeneratedSummary(null);
        result.setSummarySource(SummarySource.MANUAL);
        result.setModelName(null);
        result.setPromptVersion(null);
    }

    /**
     * Checks whether a text value is null or contains only whitespace.
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}