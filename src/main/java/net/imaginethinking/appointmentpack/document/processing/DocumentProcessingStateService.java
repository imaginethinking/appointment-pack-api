package net.imaginethinking.appointmentpack.document.processing;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.document.*;
import net.imaginethinking.appointmentpack.patientcareraccess.PatientAccessControlService;
import net.imaginethinking.appointmentpack.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessingStateService {

    private final DocumentRepository documentRepository;
    private final DocumentProcessingResultRepository processingResultRepository;
    private final RedactionContextFactory redactionContextFactory;
    private final PatientAccessControlService patientAccessControlService;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public DocumentProcessingResultResponse getProcessing(UUID authenticatedUserId, UUID documentId) {
        Document document = findAvailableDocument(documentId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.VIEW);

        DocumentProcessingResult result = findProcessingResult(documentId);

        return toResponse(document, result);
    }

    @Transactional
    public DocumentExtractionContext beginExtraction(UUID authenticatedUserId, UUID documentId) {
        Document document = findAvailableDocument(documentId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.EDIT);

        validateExtractionCanBegin(document);

        RedactionContext redactionContext = redactionContextFactory.create(document);

        document.setStatus(DocumentStatus.EXTRACTING);
        document.setProcessingFailureReason(null);

        return new DocumentExtractionContext(
                document.getId(),
                document.getDocumentType(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getStoragePath(),
                redactionContext);
    }

    @Transactional
    public DocumentProcessingResultResponse completeExtraction(UUID documentId, DocumentExtractionResponse response) {
        Document document = findAvailableDocument(documentId);

        if (document.getStatus() != DocumentStatus.EXTRACTING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document extraction is not in progress");
        }

        validateExtractionResponse(document, response);

        DocumentProcessingResult result = saveExtractionResult(document, response);

        document.setStatus(determineReviewStatus(document.getDocumentType()));

        document.setProcessingFailureReason(null);

        return toResponse(document, result);
    }

    @Transactional
    public void failExtraction(UUID documentId, String failureReason) {
        documentRepository.findById(documentId)
                .filter(document -> document.getStatus() == DocumentStatus.EXTRACTING)
                .ifPresent(document -> {
                    document.setStatus(DocumentStatus.EXTRACTION_FAILED);

                    document.setProcessingFailureReason(failureReason);
                });
    }

    @Transactional
    public DocumentSummarisationContext beginSummarisation(
            UUID authenticatedUserId,
            UUID documentId,
            String approvedDeidentifiedText) {
        Document document = findAvailableDocument(documentId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.EDIT);

        if (document.getDocumentType() != DocumentType.CONSULTATION_OUTCOME_LETTER) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only consultation outcome letters require external summarisation");
        }

        DocumentProcessingResult result = findProcessingResult(documentId);

        if (document.getStatus() == DocumentStatus.READY_FOR_DEIDENTIFICATION_REVIEW) {
            approveDeidentifiedText(result, authenticatedUserId, approvedDeidentifiedText);
        } else if (document.getStatus() == DocumentStatus.SUMMARISATION_FAILED) {
            validateRetrySnapshot(result, approvedDeidentifiedText);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Document cannot be summarised in its current status");
        }

        document.setStatus(DocumentStatus.SUMMARISING);
        document.setProcessingFailureReason(null);

        return new DocumentSummarisationContext(document.getId(), result.getApprovedDeidentifiedText());
    }

    @Transactional
    public DocumentProcessingResultResponse completeSummarisation(UUID documentId, DocumentSummaryResponse response) {
        Document document = findAvailableDocument(documentId);

        if (document.getStatus() != DocumentStatus.SUMMARISING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document summarisation is not in progress");
        }

        validateSummaryResponse(document, response);

        DocumentProcessingResult result = findProcessingResult(documentId);

        result.setGeneratedSummary(response.summary());
        result.setReviewedSummary(null);
        result.setSummarySource(SummarySource.OPENAI);
        result.setProcessorVersion(response.processorVersion());
        result.setModelName(response.modelName());
        result.setPromptVersion(response.promptVersion());

        document.setStatus(DocumentStatus.READY_FOR_SUMMARY_REVIEW);
        document.setProcessingFailureReason(null);

        return toResponse(document, result);
    }

    @Transactional
    public void failSummarisation(UUID documentId, String failureReason) {
        documentRepository.findById(documentId)
                .filter(document -> document.getStatus() == DocumentStatus.SUMMARISING)
                .ifPresent(document -> {
                    document.setStatus(DocumentStatus.SUMMARISATION_FAILED);

                    document.setProcessingFailureReason(failureReason);
                });
    }

    private void approveDeidentifiedText(
            DocumentProcessingResult result,
            UUID authenticatedUserId,
            String approvedDeidentifiedText) {
        if (isBlank(approvedDeidentifiedText)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approved de-identified text is required");
        }

        result.setApprovedDeidentifiedText(approvedDeidentifiedText);

        result.setDeidentificationReviewedBy(entityManager.getReference(User.class, authenticatedUserId));

        result.setDeidentificationReviewedAt(Instant.now());
    }

    private void validateRetrySnapshot(DocumentProcessingResult result, String approvedDeidentifiedText) {
        if (result.getApprovedDeidentifiedText() == null || !result.getApprovedDeidentifiedText()
                .equals(approvedDeidentifiedText)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The approved de-identified text cannot be changed during retry");
        }
    }

    private DocumentProcessingResult saveExtractionResult(Document document, DocumentExtractionResponse response) {
        DocumentProcessingResult result = processingResultRepository.findByDocumentId(document.getId())
                .orElseGet(DocumentProcessingResult::new);

        result.setDocument(document);
        result.setExtractedText(response.extractedText());
        result.setMachineDeidentifiedText(response.deidentifiedText());
        result.setApprovedDeidentifiedText(null);
        result.setGeneratedSummary(response.generatedSummary());
        result.setReviewedSummary(null);
        result.setProcessingWarning(response.processingWarning());
        result.setProcessorVersion(response.processorVersion());
        result.setModelName(null);
        result.setPromptVersion(null);
        result.setDeidentificationReviewedBy(null);
        result.setDeidentificationReviewedAt(null);

        if (document.getDocumentType() == DocumentType.APPOINTMENT_LETTER) {
            result.setSummarySource(SummarySource.DETERMINISTIC);
        } else {
            result.setSummarySource(null);
        }

        return processingResultRepository.save(result);
    }

    private void validateExtractionCanBegin(
            Document document) {
        boolean extractable = document.getStatus() == DocumentStatus.UPLOADED || document.getStatus() == DocumentStatus.EXTRACTION_FAILED;

        if (!extractable) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Document cannot be extracted in its current status");
        }
    }

    private void validateExtractionResponse(Document document, DocumentExtractionResponse response) {
        if (response == null) {
            throw new DocumentProcessingException("Document extraction service returned an empty response");
        }

        if (!document.getId().equals(response.documentId())) {
            throw new DocumentProcessingException("Document extraction response contains an unexpected document ID");
        }

        if (isBlank(response.extractedText())) {
            throw new DocumentProcessingException("Document extraction response contains no extracted text");
        }

        if (isBlank(response.processorVersion())) {
            throw new DocumentProcessingException("Document extraction response contains no processor version");
        }

        if (document.getDocumentType() == DocumentType.APPOINTMENT_LETTER && isBlank(response.generatedSummary())) {
            throw new DocumentProcessingException("Appointment extraction response contains no generated summary");
        }

        if (document.getDocumentType() == DocumentType.CONSULTATION_OUTCOME_LETTER && isBlank(response.deidentifiedText())) {
            throw new DocumentProcessingException("Consultation extraction response contains no de-identified text");
        }
    }

    private void validateSummaryResponse(Document document, DocumentSummaryResponse response) {
        if (response == null) {
            throw new DocumentProcessingException("Document summarisation service returned an empty response");
        }

        if (!document.getId().equals(response.documentId())) {
            throw new DocumentProcessingException("Document summarisation response contains an unexpected document ID");
        }

        if (isBlank(response.summary()) || isBlank(response.processorVersion()) || isBlank(response.modelName()) || isBlank(
                response.promptVersion())) {
            throw new DocumentProcessingException("Document summarisation response is incomplete");
        }
    }

    private DocumentStatus determineReviewStatus(
            DocumentType documentType) {
        return switch (documentType) {
            case APPOINTMENT_LETTER -> DocumentStatus.READY_FOR_SUMMARY_REVIEW;

            case CONSULTATION_OUTCOME_LETTER -> DocumentStatus.READY_FOR_DEIDENTIFICATION_REVIEW;
        };
    }

    private DocumentProcessingResult findProcessingResult(
            UUID documentId) {
        return processingResultRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new DocumentProcessingException("Document processing result was not found"));
    }

    private Document findAvailableDocument(
            UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }

        return document;
    }

    private DocumentProcessingResultResponse toResponse(Document document, DocumentProcessingResult result) {
        DocumentProcessingResultResponse.ModelMetadata model = result.getModelName() == null ? null : new DocumentProcessingResultResponse.ModelMetadata(result.getModelName(),
                result.getPromptVersion());

        UUID reviewerId = result.getDeidentificationReviewedBy() == null ? null : result.getDeidentificationReviewedBy()
                .getId();

        return new DocumentProcessingResultResponse(
                document.getId(),
                document.getDocumentType(),
                document.getStatus(),
                result.getExtractedText(),
                result.getMachineDeidentifiedText(),
                result.getApprovedDeidentifiedText(),
                result.getGeneratedSummary(),
                result.getReviewedSummary(),
                result.getSummarySource(),
                result.getProcessingWarning(),
                result.getProcessorVersion(),
                model,
                reviewerId,
                result.getDeidentificationReviewedAt());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}