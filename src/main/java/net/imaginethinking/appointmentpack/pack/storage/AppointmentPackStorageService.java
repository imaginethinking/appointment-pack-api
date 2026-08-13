package net.imaginethinking.appointmentpack.pack.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class AppointmentPackStorageService {

    private final Path storageDirectory;

    public AppointmentPackStorageService(
            @Value("${appointment-pack.appointment-packs.storage-directory}")
            String storageDirectory) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();

        initialiseStorageDirectory();
    }

    public String store(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("Appointment pack PDF must not be empty");
        }

        String storedFileName = UUID.randomUUID() + ".pdf";
        String directoryName = storedFileName.substring(0, 2);

        Path relativePath = Path.of(directoryName, storedFileName);

        Path targetPath = resolve(relativePath.toString());

        try {
            Files.createDirectories(targetPath.getParent());

            Files.write(targetPath, pdfBytes, StandardOpenOption.CREATE_NEW);

            return relativePath.toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store appointment pack", exception);
        }
    }

    public Resource load(String storagePath) {
        Path filePath = resolve(storagePath);

        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("Stored appointment pack is not readable");
            }

            return resource;
        } catch (MalformedURLException exception) {
            throw new IllegalStateException("Failed to load appointment pack", exception);
        }
    }

    public void delete(String storagePath) {
        Path filePath = resolve(storagePath);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete appointment pack", exception);
        }
    }

    private Path resolve(String storagePath) {
        Path resolvedPath = storageDirectory.resolve(storagePath).normalize();

        if (!resolvedPath.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid appointment pack storage path");
        }

        return resolvedPath;
    }

    private void initialiseStorageDirectory() {
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialise appointment pack storage", exception);
        }
    }
}