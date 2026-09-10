package net.imaginethinking.appointmentpack.document.processing;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.document.Document;
import net.imaginethinking.appointmentpack.document.DocumentRepository;
import net.imaginethinking.appointmentpack.document.DocumentStatus;
import net.imaginethinking.appointmentpack.document.processing.client.DocumentProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

/**
 * Loads documents and their processing records for the document processing workflow.
 */
@Service
@RequiredArgsConstructor
public class DocumentProcessingRecordService {

    private final DocumentRepository documentRepository;
    private final DocumentProcessingResultRepository processingResultRepository;

    /**
     * Loads a document and treats archived documents as unavailable to the processing workflow.
     */
    @Transactional(readOnly = true)
    public Document requireAvailableDocument(
            UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found")
                );

        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Document not found"
            );
        }

        return document;
    }

    /**
     * Loads the processing result saved for a document when one exists.
     */
    @Transactional(readOnly = true)
    public Optional<DocumentProcessingResult> findProcessingResult(UUID documentId) {
        return processingResultRepository.findByDocumentId(documentId);
    }

    /**
     * Loads the processing result or fails when the document has not produced one yet.
     */
    @Transactional(readOnly = true)
    public DocumentProcessingResult requireProcessingResult(
            UUID documentId) {
        return findProcessingResult(documentId).orElseThrow(() -> new DocumentProcessingException(
                "Document processing result was not found"));
    }
}