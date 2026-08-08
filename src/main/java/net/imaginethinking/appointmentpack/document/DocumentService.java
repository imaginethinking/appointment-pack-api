package net.imaginethinking.appointmentpack.document;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.document.storage.DocumentStorageService;
import net.imaginethinking.appointmentpack.patientcareraccess.PatientAccessControlService;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordRepository;
import net.imaginethinking.appointmentpack.user.User;
import net.imaginethinking.appointmentpack.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<DocumentStatus> ARCHIVABLE_STATUSES = Set.of(
            DocumentStatus.UPLOADED,
            DocumentStatus.READY_FOR_DEIDENTIFICATION_REVIEW,
            DocumentStatus.READY_FOR_SUMMARY_REVIEW,
            DocumentStatus.EXTRACTION_FAILED,
            DocumentStatus.SUMMARISATION_FAILED,
            DocumentStatus.REJECTED);

    private final DocumentRepository documentRepository;
    private final PatientRecordRepository patientRecordRepository;
    private final UserRepository userRepository;
    private final PatientAccessControlService patientAccessControlService;
    private final DocumentFileValidator documentFileValidator;
    private final DocumentStorageService documentStorageService;

    @Transactional
    public DocumentResponse upload(
            UUID authenticatedUserId,
            UUID patientRecordId,
            DocumentType documentType,
            MultipartFile file) {
        if (documentType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document type must be provided");
        }

        PatientRecord patientRecord = findPatientRecord(patientRecordId);

        patientAccessControlService.requirePermission(authenticatedUserId, patientRecord, DocumentPermission.UPLOAD);

        User uploadedBy = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"));

        String contentType = documentFileValidator.validateAndGetContentType(file);

        String originalFileName = resolveOriginalFileName(file);
        String storagePath = null;

        try {
            storagePath = documentStorageService.store(file);

            Document document = new Document();
            document.setPatientRecord(patientRecord);
            document.setUploadedBy(uploadedBy);
            document.setDocumentType(documentType);
            document.setStatus(DocumentStatus.UPLOADED);
            document.setOriginalFileName(originalFileName);
            document.setStoredFileName(Path.of(storagePath).getFileName().toString());
            document.setContentType(contentType);
            document.setFileSize(file.getSize());
            document.setStoragePath(storagePath);

            Document savedDocument = documentRepository.saveAndFlush(document);

            return toResponse(savedDocument);
        } catch (RuntimeException exception) {
            deleteStoredFileAfterFailure(storagePath, exception);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(UUID authenticatedUserId, UUID patientRecordId) {
        PatientRecord patientRecord = findPatientRecord(patientRecordId);

        patientAccessControlService.requirePermission(authenticatedUserId, patientRecord, DocumentPermission.VIEW);

        return documentRepository.findAllByPatientRecordIdAndStatusNotOrderByCreatedAtDesc(
                patientRecordId,
                DocumentStatus.ARCHIVED).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID authenticatedUserId, UUID documentId) {
        Document document = findAvailableDocument(documentId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.VIEW);

        return toResponse(document);
    }

    @Transactional(readOnly = true)
    public DocumentDownload download(UUID authenticatedUserId, UUID documentId) {
        Document document = findAvailableDocument(documentId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.VIEW);

        return new DocumentDownload(
                documentStorageService.load(document.getStoragePath()),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getFileSize());
    }

    @Transactional
    public DocumentResponse archive(UUID authenticatedUserId, UUID documentId) {
        Document document = findDocument(documentId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.EDIT);

        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            return toResponse(document);
        }

        if (!ARCHIVABLE_STATUSES.contains(document.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Document cannot be archived in its current status"
            );
        }

        document.setStatus(DocumentStatus.ARCHIVED);

        return toResponse(document);
    }

    private PatientRecord findPatientRecord(UUID patientRecordId) {
        return patientRecordRepository.findById(patientRecordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient record not found"));
    }

    private Document findDocument(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    private Document findAvailableDocument(UUID documentId) {
        Document document = findDocument(documentId);

        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }

        return document;
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getPatientRecord().getId(),
                document.getDocumentType(),
                document.getStatus(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getFileSize(),
                document.getCreatedAt());
    }

    private String resolveOriginalFileName(MultipartFile file) {
        String suppliedFileName = file.getOriginalFilename();

        if (!StringUtils.hasText(suppliedFileName)) {
            return "document";
        }

        String cleanedPath = StringUtils.cleanPath(suppliedFileName);
        String fileName = StringUtils.getFilename(cleanedPath);

        if (!StringUtils.hasText(fileName)) {
            return "document";
        }

        return fileName.length() <= 255 ? fileName : fileName.substring(0, 255);
    }

    private void deleteStoredFileAfterFailure(String storagePath, RuntimeException originalException) {
        if (storagePath == null) {
            return;
        }

        try {
            documentStorageService.delete(storagePath);
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }
}
