package net.imaginethinking.appointmentpack.document.processing;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.document.Document;
import net.imaginethinking.appointmentpack.document.DocumentPermission;
import net.imaginethinking.appointmentpack.document.DocumentRepository;
import net.imaginethinking.appointmentpack.document.DocumentStatus;
import net.imaginethinking.appointmentpack.document.storage.DocumentStorageService;
import net.imaginethinking.appointmentpack.patientcareraccess.PatientAccessControlService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private static final String EXTRACTION_FAILURE_MESSAGE = "Document text could not be extracted";

    private final DocumentRepository documentRepository;
    private final DocumentProcessingResultRepository processingResultRepository;
    private final DocumentStorageService documentStorageService;
    private final DocumentProcessingClient documentProcessingClient;
    private final PatientAccessControlService patientAccessControlService;

    @Transactional(
            noRollbackFor = DocumentProcessingException.class
    )
    public DocumentProcessingResultResponse extract(
            UUID authenticatedUserId,
            UUID documentId
    ) {
        Document document = findAvailableDocument(documentId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.UPLOAD
        );

        validateExtractionStatus(document);

        document.setStatus(DocumentStatus.PROCESSING);
        document.setProcessingFailureReason(null);

        Resource resource = documentStorageService.load(document.getStoragePath());

        try {
            DocumentExtractionResponse extractionResponse = documentProcessingClient.extract(
                    document,
                    resource
            );

            DocumentProcessingResult result = saveExtractionResult(
                    document,
                    extractionResponse
            );

            document.setStatus(DocumentStatus.READY_FOR_REVIEW);

            return toResponse(document, result);
        } catch (DocumentProcessingException exception) {
            document.setStatus(DocumentStatus.FAILED);
            document.setProcessingFailureReason(EXTRACTION_FAILURE_MESSAGE);

            throw exception;
        }
    }

    private DocumentProcessingResult saveExtractionResult(
            Document document,
            DocumentExtractionResponse response
    ) {
        DocumentProcessingResult result =
                processingResultRepository
                        .findByDocumentId(document.getId())
                        .orElseGet(
                                DocumentProcessingResult::new
                        );

        result.setDocument(document);
        result.setExtractedText(valueOrEmpty(response.extractedText()));
        result.setGeneratedSummary(valueOrEmpty(response.generatedSummary()));
        result.setReviewedSummary(null);
        result.setProcessingWarning(response.processingWarning());
        result.setProcessorVersion(response.processorVersion());
        result.setModelName(null);
        result.setModelRevision(null);

        return processingResultRepository.save(result);
    }

    private void validateExtractionStatus(
            Document document
    ) {
        boolean extractable = document.getStatus() == DocumentStatus.UPLOADED || document.getStatus() == DocumentStatus.FAILED;

        if (!extractable) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document cannot be extracted in its current status");
        }
    }

    private Document findAvailableDocument(
            UUID documentId
    ) {
        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
                );

        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Document not found"
            );
        }

        return document;
    }

    private DocumentProcessingResultResponse toResponse(
            Document document,
            DocumentProcessingResult result
    ) {
        DocumentProcessingResultResponse.ModelMetadata model =
                result.getModelName() == null
                        ? null
                        : new DocumentProcessingResultResponse
                        .ModelMetadata(
                        result.getModelName(),
                        result.getModelRevision()
                );

        return new DocumentProcessingResultResponse(
                document.getId(),
                document.getStatus(),
                result.getExtractedText(),
                result.getGeneratedSummary(),
                result.getReviewedSummary(),
                result.getProcessingWarning(),
                result.getProcessorVersion(),
                model
        );
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}