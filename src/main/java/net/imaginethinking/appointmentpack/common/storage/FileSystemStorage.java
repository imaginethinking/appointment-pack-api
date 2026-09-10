package net.imaginethinking.appointmentpack.common.storage;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.stream.StreamSupport;

/**
 * Stores and retrieves files underneath a configured root directory while keeping paths inside that directory.
 */
public final class FileSystemStorage {

    private final Path rootDirectory;

    /**
     * Creates the file storage helper using the configured storage directory.
     */
    public FileSystemStorage(String storageDirectory) {
        if (storageDirectory == null || storageDirectory.isBlank()) {
            throw new IllegalArgumentException("Storage directory must not be blank");
        }

        rootDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();

        initialiseRootDirectory();
    }

    /**
     * Writes the supplied content to a checked path underneath the storage root and returns the portable relative
     * path.
     */
    public String store(InputStream inputStream, String storagePath) {
        Objects.requireNonNull(inputStream, "Input stream must not be null");

        Path targetPath = resolve(storagePath);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath);

            return toPortableStoragePath(targetPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store file", exception);
        }
    }

    /**
     * Writes the content to a checked path underneath the storage root and returns the portable relative
     * path.
     */
    public String store(byte[] bytes, String storagePath) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("File content must not be empty");
        }

        Path targetPath = resolve(storagePath);

        try {
            Files.createDirectories(targetPath.getParent());

            Files.write(targetPath, bytes, StandardOpenOption.CREATE_NEW);

            return toPortableStoragePath(targetPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store file", exception);
        }
    }

    /**
     * Resolves the requested storage path and returns it as a readable file resource.
     */
    public Resource load(String storagePath) {
        Path filePath = resolve(storagePath);

        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("Stored file is not readable");
            }

            return resource;
        } catch (MalformedURLException exception) {
            throw new IllegalStateException("Failed to load stored file", exception);
        }
    }

    /**
     * Deletes the stored file when it exists while keeping the requested path inside the storage root.
     */
    public void delete(String storagePath) {
        Path filePath = resolve(storagePath);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete stored file", exception);
        }
    }

    /**
     * Normalises a storage path and rejects any value that would escape the configured storage directory.
     */
    private Path resolve(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException("Storage path must not be blank");
        }

        Path relativePath = Path.of(storagePath);

        if (relativePath.isAbsolute()) {
            throw new IllegalArgumentException("Storage path must be relative");
        }

        Path resolvedPath = rootDirectory.resolve(relativePath).normalize();

        // Reject paths that escape the configured storage directory after normalisation.
        if (!resolvedPath.startsWith(rootDirectory) || resolvedPath.equals(rootDirectory)) {
            throw new IllegalArgumentException("Invalid storage path");
        }

        return resolvedPath;
    }

    /**
     * Converts a stored absolute path into a forward slash relative path that can be saved consistently.
     */
    private String toPortableStoragePath(Path absolutePath) {
        Path relativePath = rootDirectory.relativize(absolutePath);

        return StreamSupport.stream(relativePath.spliterator(), false)
                .map(Path::toString)
                .reduce((left, right) -> left + "/" + right)
                .orElseThrow(() -> new IllegalStateException("Stored file path could not be resolved"));
    }

    /**
     * Creates the configured storage directory when it does not already exist.
     */
    private void initialiseRootDirectory() {
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialise storage directory", exception);
        }
    }
}