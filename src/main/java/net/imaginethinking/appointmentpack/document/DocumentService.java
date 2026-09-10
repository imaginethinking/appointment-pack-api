package net.imaginethinking.appointmentpack.document;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.document.storage.DocumentStorageService;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecord;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
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

/**
 * Stores uploaded documents, keeps their metadata and applies patient access checks when they are viewed or
 * archived.
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<DocumentStatus> ARCHIVABLE_STATUSES = Set.of(
            DocumentStatus.UPLOADED,
            DocumentStatus.READY_FOR_APPOINTMENT_REVIEW,
            DocumentStatus.READY_FOR_DEIDENTIFICATION_REVIEW,
            DocumentStatus.READY_FOR_SUMMARY_REVIEW,
            DocumentStatus.EXTRACTION_FAILED,
            DocumentStatus.SUMMARISATION_FAILED,
            DocumentStatus.REJECTED,
            DocumentStatus.ACCEPTED);

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final PatientRecordAccessService patientRecordAccessService;
    private final DocumentFileValidator documentFileValidator;
    private final DocumentStorageService documentStorageService;
    private final AppEventPublisher appEventPublisher;

    /**
     * Checks upload permission and the file contents, stores the file, then saves the document metadata for the
     * patient.
     */
    @Transactional
    public DocumentResponse upload(
            UUID authenticatedUserId,
            UUID patientRecordId,
            DocumentType documentType,
            MultipartFile file) {
        if (documentType == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document type must be provided");
        }

        PatientRecord patientRecord = patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecordId,
                DocumentPermission.UPLOAD);

        User uploadedBy = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"));

        String contentType = documentFileValidator.validateAndGetContentType(file);

        String originalFileName = resolveOriginalFileName(file);
        String storagePath = null;

        // Remove the stored file again if saving its database record fails so an unused upload is not left behind.
        try {
            storagePath = documentStorageService.store(file, contentType);

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

            publishActivity(authenticatedUserId, savedDocument, PatientActivityAction.UPLOADED);

            return toResponse(savedDocument);
        } catch (RuntimeException exception) {
            deleteStoredFileAfterFailure(storagePath, exception);
            throw exception;
        }
    }

    /**
     * Checks view access and returns the active documents for the patient with the newest uploads first.
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(UUID authenticatedUserId, UUID patientRecordId) {
        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecordId,
                DocumentPermission.VIEW);

        return documentRepository.findAllByPatientRecordIdAndStatusNotOrderByCreatedAtDesc(
                patientRecordId,
                DocumentStatus.ARCHIVED).stream().map(this::toResponse).toList();
    }

    /**
     * Loads an active document and checks that the current user can view its patient record.
     */
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID authenticatedUserId, UUID documentId) {
        Document document = findAvailableDocument(documentId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.VIEW);

        return toResponse(document);
    }

    /**
     * Loads an active document, checks view access and returns the stored original file for download.
     */
    @Transactional
    public DocumentDownload download(UUID authenticatedUserId, UUID documentId) {
        Document document = findAvailableDocument(documentId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.VIEW);

        DocumentDownload download = new DocumentDownload(
                documentStorageService.load(document.getStoragePath()),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getFileSize());

        publishActivity(authenticatedUserId, document, PatientActivityAction.DOWNLOADED);

        return download;
    }

    /**
     * Checks edit access and archives the document if it is still active.
     */
    @Transactional
    public DocumentResponse archive(UUID authenticatedUserId, UUID documentId) {
        Document document = findDocument(documentId);

        patientRecordAccessService.requireAccess(
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

        publishActivity(authenticatedUserId, document, PatientActivityAction.ARCHIVED);

        return toResponse(document);
    }

    /**
     * Publishes the activity event for the completed change.
     */
    private void publishActivity(UUID authenticatedUserId, Document document, PatientActivityAction action) {
        appEventPublisher.publish(PatientActivityEvent.create(
                authenticatedUserId,
                document.getPatientRecord().getId(),
                PatientResourceType.DOCUMENT,
                document.getId(),
                action));
    }

    /**
     * Loads a document by ID or returns not found when it does not exist.
     */
    private Document findDocument(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found")
                );
    }

    /**
     * Loads a document and treats archived documents as not found.
     */
    private Document findAvailableDocument(UUID documentId) {
        Document document = findDocument(documentId);

        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Document not found"
            );
        }

        return document;
    }

    /**
     * Builds the response from the supplied document.
     */
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

    /**
     * Keeps a usable original file name and falls back to a simple name when the upload does not provide one.
     */
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

    /**
     * Removes a newly stored file when saving its document record fails and keeps any cleanup error with the
     * original failure.
     */
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