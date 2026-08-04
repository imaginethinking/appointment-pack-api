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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private static final String PROCESSING_FAILURE_MESSAGE = "The document could not be processed";

    private final DocumentRepository documentRepository;
    private final DocumentProcessingResultRepository processingResultRepository;
    private final DocumentStorageService documentStorageService;
    private final DocumentProcessingClient documentProcessingClient;
    private final PatientAccessControlService patientAccessControlService;

    @Transactional(
            noRollbackFor = DocumentProcessingException.class
    )
    public DocumentProcessingResultResponse process(
            UUID authenticatedUserId,
            UUID documentId
    ) {
        Document document = findAvailableDocument(documentId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.UPLOAD
        );

        validateProcessingStatus(document);

        document.setStatus(DocumentStatus.PROCESSING);
        document.setProcessingFailureReason(null);

        Resource resource = documentStorageService.load(document.getStoragePath());

        try {
            DocumentProcessingResponse processingResponse =
                    documentProcessingClient.process(
                            document,
                            resource
                    );

            DocumentProcessingResult result =
                    saveProcessingResult(
                            document,
                            processingResponse
                    );

            document.setStatus(DocumentStatus.READY_FOR_REVIEW);

            return toResponse(document, result);
        } catch (DocumentProcessingException exception) {
            document.setStatus(DocumentStatus.FAILED);
            document.setProcessingFailureReason(
                    PROCESSING_FAILURE_MESSAGE
            );

            throw exception;
        }
    }

    private DocumentProcessingResult saveProcessingResult(
            Document document,
            DocumentProcessingResponse response
    ) {
        DocumentProcessingResult result =
                processingResultRepository
                        .findByDocumentId(document.getId())
                        .orElseGet(DocumentProcessingResult::new);

        result.setDocument(document);
        result.setExtractedText(valueOrEmpty(response.extractedText()));
        result.setGeneratedSummary(valueOrEmpty(response.summary()));
        result.setReviewedSummary(null);
        result.setProcessingWarning(response.processingWarning());
        result.setProcessorVersion(response.processorVersion());

        if (response.model() == null) {
            result.setModelName(null);
            result.setModelRevision(null);
        } else {
            result.setModelName(response.model().name());
            result.setModelRevision(
                    response.model().revision()
            );
        }

        return processingResultRepository.save(result);
    }

    private void validateProcessingStatus(Document document) {
        boolean processable = document.getStatus() == DocumentStatus.UPLOADED || document.getStatus() == DocumentStatus.FAILED;

        if (!processable) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Document cannot be processed in its current status"
            );
        }
    }

    private Document findAvailableDocument(UUID documentId) {
        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found"
                ));

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
                        : new DocumentProcessingResultResponse.ModelMetadata(
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