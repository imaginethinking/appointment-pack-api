package net.imaginethinking.appointmentpack.document.processing;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.document.storage.DocumentStorageService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private static final String EXTRACTION_FAILURE_MESSAGE = "Document text could not be extracted.";

    private final DocumentProcessingStateService documentProcessingStateService;
    private final DocumentStorageService documentStorageService;
    private final DocumentProcessingClient documentProcessingClient;

    public DocumentProcessingResultResponse extract(UUID authenticatedUserId, UUID documentId) {
        DocumentExtractionContext context = documentProcessingStateService.beginExtraction(
                authenticatedUserId,
                documentId);

        try {
            Resource resource = documentStorageService.load(context.storagePath());

            DocumentExtractionResponse extractionResponse = documentProcessingClient.extract(context, resource);

            return documentProcessingStateService.completeExtraction(context.documentId(), extractionResponse);
        } catch (RuntimeException exception) {
            recordExtractionFailure(context.documentId(), exception);

            throw exception;
        }
    }

    private void recordExtractionFailure(UUID documentId, RuntimeException originalException) {
        try {
            documentProcessingStateService.failExtraction(documentId, EXTRACTION_FAILURE_MESSAGE);
        } catch (RuntimeException failurePersistenceException) {
            originalException.addSuppressed(failurePersistenceException);
        }
    }
}