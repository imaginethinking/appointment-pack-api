package net.imaginethinking.appointmentpack.document.storage;

import net.imaginethinking.appointmentpack.common.storage.FileSystemStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final FileSystemStorage fileSystemStorage;

    public DocumentStorageService(
            @Value("${appointment-pack.documents.storage-directory}")
            String storageDirectory) {
        fileSystemStorage = new FileSystemStorage(storageDirectory);
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Document file must not be empty");
        }

        String storedFileName = createStoredFileName(file.getOriginalFilename());

        String storagePath = createStoragePath(storedFileName);

        try (InputStream inputStream = file.getInputStream()) {
            return fileSystemStorage.store(inputStream, storagePath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read document upload", exception);
        }
    }

    public Resource load(String storagePath) {
        return fileSystemStorage.load(storagePath);
    }

    public void delete(String storagePath) {
        fileSystemStorage.delete(storagePath);
    }

    private String createStoredFileName(String originalFileName) {
        String extension = StringUtils.getFilenameExtension(originalFileName);

        String generatedFileName = UUID.randomUUID().toString();

        if (!StringUtils.hasText(extension)) {
            return generatedFileName;
        }

        return generatedFileName + "." + extension.toLowerCase(Locale.ROOT);
    }

    private String createStoragePath(String storedFileName) {
        String directoryName = storedFileName.substring(0, 2);

        return directoryName + "/" + storedFileName;
    }
}