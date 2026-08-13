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

@Service
@RequiredArgsConstructor
public class ConsultationDocumentReviewService {

    private final DocumentProcessingRecordService processingRecordService;
    private final MedicalHistoryEntryRepository medicalHistoryEntryRepository;
    private final PatientRecordAccessService patientRecordAccessService;
    private final DocumentProcessingResultMapper processingResultMapper;
    private final EntityManager entityManager;

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

        return processingResultMapper.toResponse(document, result);
    }

    @Transactional
    public DocumentProcessingResultResponse rejectSummary(UUID authenticatedUserId, UUID documentId) {
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

        return processingResultMapper.toResponse(document, result);
    }

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

    private void validateGeneratedSummary(DocumentProcessingResult result) {
        if (isBlank(result.getGeneratedSummary()) || result.getSummarySource() == null) {
            throw new DocumentProcessingException("Document processing result contains no generated summary");
        }
    }

    private void applyManualSummary(DocumentProcessingResult result) {
        result.setGeneratedSummary(null);
        result.setSummarySource(SummarySource.MANUAL);
        result.setModelName(null);
        result.setPromptVersion(null);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}