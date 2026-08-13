package net.imaginethinking.appointmentpack.document.processing;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.document.Document;
import net.imaginethinking.appointmentpack.document.DocumentRepository;
import net.imaginethinking.appointmentpack.document.DocumentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessingRecordService {

    private final DocumentRepository documentRepository;
    private final DocumentProcessingResultRepository processingResultRepository;

    @Transactional(readOnly = true)
    public Document requireAvailableDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }

        return document;
    }

    @Transactional(readOnly = true)
    public DocumentProcessingResult requireProcessingResult(
            UUID documentId) {
        return processingResultRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new DocumentProcessingException("Document processing result was not found"));
    }
}