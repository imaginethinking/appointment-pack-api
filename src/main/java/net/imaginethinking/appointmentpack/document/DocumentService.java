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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {
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
            MultipartFile file
    ) {
        if (documentType == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document type must be provided"
            );
        }

        PatientRecord patientRecord = patientRecordRepository
                .findById(patientRecordId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient record not found"
                ));

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                patientRecord,
                DocumentPermission.UPLOAD
        );

        User uploadedBy = userRepository
                .findById(authenticatedUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));


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
            document.setStoredFileName(
                    Path.of(storagePath)
                            .getFileName()
                            .toString()
            );
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

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getPatientRecord().getId(),
                document.getDocumentType(),
                document.getStatus(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getFileSize()
        );
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

        return fileName.length() <= 255
                ? fileName
                : fileName.substring(0, 255);
    }

    private void deleteStoredFileAfterFailure(
            String storagePath,
            RuntimeException originalException
    ) {
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
