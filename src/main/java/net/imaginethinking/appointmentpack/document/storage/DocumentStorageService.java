package net.imaginethinking.appointmentpack.document.storage;

import net.imaginethinking.appointmentpack.common.storage.FileSystemStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Stores uploaded document files using generated names underneath the document storage directory.
 */
@Service
public class DocumentStorageService {

    private final FileSystemStorage fileSystemStorage;

    /**
     * Creates the document storage helper using the configured storage directory.
     */
    public DocumentStorageService(
            @Value("${appointment-pack.documents.storage-directory}")
            String storageDirectory) {
        fileSystemStorage = new FileSystemStorage(storageDirectory);
    }

    /**
     * Generates a storage name from the file type and writes the uploaded document under the document directory.
     */
    public String store(MultipartFile file, String contentType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Document file must not be empty");
        }

        String storedFileName = createStoredFileName(contentType);

        String storagePath = createStoragePath(storedFileName);

        try (InputStream inputStream = file.getInputStream()) {
            return fileSystemStorage.store(inputStream, storagePath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read document upload", exception);
        }
    }

    /**
     * Loads a stored document from its saved relative path.
     */
    public Resource load(String storagePath) {
        return fileSystemStorage.load(storagePath);
    }

    /**
     * Deletes the stored document at the supplied relative path.
     */
    public void delete(String storagePath) {
        fileSystemStorage.delete(storagePath);
    }

    /**
     * Creates a random stored file name while keeping the extension that matches the validated content type.
     */
    private String createStoredFileName(String contentType) {
        String extension = switch (contentType) {
            case MediaType.APPLICATION_PDF_VALUE -> "pdf";
            case MediaType.IMAGE_PNG_VALUE -> "png";
            case MediaType.IMAGE_JPEG_VALUE -> "jpg";

            default ->
                    throw new IllegalArgumentException("Unsupported validated document content type: " + contentType);
        };

        return UUID.randomUUID() + "." + extension;
    }

    /**
     * Places the generated document file name underneath the document storage folder.
     */
    private String createStoragePath(String storedFileName) {
        String directoryName = storedFileName.substring(0, 2);

        return directoryName + "/" + storedFileName;
    }
}