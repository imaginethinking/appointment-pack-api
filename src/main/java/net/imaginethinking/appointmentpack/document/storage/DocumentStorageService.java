package net.imaginethinking.appointmentpack.document.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentStorageService {
    private final Path storageDirectory;

    public DocumentStorageService(
            @Value("${appointment-pack.documents.storage-directory}")
            String storageDirectory
    ) {
        this.storageDirectory = Path.of(storageDirectory)
                .toAbsolutePath()
                .normalize();

        initialiseStorageDirectory();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Document file must not be empty");
        }

        String storedFilename = createStoredFilename(file.getOriginalFilename());
        String directoryName = storedFilename.substring(0, 2);
        Path relativePath = Path.of(directoryName, storedFilename);
        Path targetPath = resolve(relativePath.toString());

        try {
            Files.createDirectories(targetPath.getParent());

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(
                        inputStream,
                        targetPath,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return relativePath.toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store document", exception);
        }
    }

    public Resource load(String storagePath) {
        Path filePath = resolve(storagePath);

        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("Stored document is not readable");
            }

            return resource;
        } catch (MalformedURLException exception) {
            throw new IllegalStateException("Failed to load document", exception);
        }
    }

    public void delete(String storagePath) {
        Path filePath = resolve(storagePath);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete document", exception);
        }
    }

    private String createStoredFilename(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String generatedFilename = UUID.randomUUID().toString();

        if (!StringUtils.hasText(extension)) {
            return generatedFilename;
        }

        return generatedFilename
                + "."
                + extension.toLowerCase(Locale.ROOT);
    }

    private Path resolve(String storagePath) {
        Path resolvedPath = storageDirectory
                .resolve(storagePath)
                .normalize();

        if (!resolvedPath.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid document storage path");
        }

        return resolvedPath;
    }

    private void initialiseStorageDirectory() {
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to initialise document storage",
                    exception
            );
        }
    }
}
