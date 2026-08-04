package net.imaginethinking.appointmentpack.document.processing;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.document.*;
import net.imaginethinking.appointmentpack.patientcareraccess.PatientAccessControlService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessingStateService {

    private final DocumentRepository documentRepository;
    private final DocumentProcessingResultRepository processingResultRepository;
    private final RedactionContextFactory redactionContextFactory;
    private final PatientAccessControlService patientAccessControlService;

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

    private DocumentProcessingResult saveExtractionResult(Document document, DocumentExtractionResponse response) {
        DocumentProcessingResult result = processingResultRepository.findByDocumentId(document.getId())
                .orElseGet(DocumentProcessingResult::new);

        result.setDocument(document);
        result.setExtractedText(response.extractedText());
        result.setMachineDeidentifiedText(response.deidentifiedText());
        result.setGeneratedSummary(response.generatedSummary());
        result.setReviewedSummary(null);
        result.setProcessingWarning(response.processingWarning());
        result.setProcessorVersion(response.processorVersion());
        result.setModelName(null);
        result.setModelRevision(null);

        return processingResultRepository.save(result);
    }

    private void validateExtractionCanBegin(Document document) {
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

    private DocumentStatus determineReviewStatus(DocumentType documentType) {
        return switch (documentType) {
            case APPOINTMENT_LETTER -> DocumentStatus.READY_FOR_SUMMARY_REVIEW;

            case CONSULTATION_OUTCOME_LETTER -> DocumentStatus.READY_FOR_DEIDENTIFICATION_REVIEW;
        };
    }

    private Document findAvailableDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }

        return document;
    }

    private DocumentProcessingResultResponse toResponse(Document document, DocumentProcessingResult result) {
        DocumentProcessingResultResponse.ModelMetadata model = result.getModelName() == null ? null : new DocumentProcessingResultResponse.ModelMetadata(
                result.getModelName(),
                result.getModelRevision());

        return new DocumentProcessingResultResponse(
                document.getId(),
                document.getStatus(),
                result.getExtractedText(),
                result.getMachineDeidentifiedText(),
                result.getGeneratedSummary(),
                result.getReviewedSummary(),
                result.getProcessingWarning(),
                result.getProcessorVersion(),
                model);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}