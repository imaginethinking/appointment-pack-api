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

public final class FileSystemStorage {

    private final Path rootDirectory;

    public FileSystemStorage(String storageDirectory) {
        if (storageDirectory == null || storageDirectory.isBlank()) {
            throw new IllegalArgumentException("Storage directory must not be blank");
        }

        rootDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();

        initialiseRootDirectory();
    }

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

    public void delete(String storagePath) {
        Path filePath = resolve(storagePath);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete stored file", exception);
        }
    }

    private Path resolve(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException("Storage path must not be blank");
        }

        Path relativePath = Path.of(storagePath);

        if (relativePath.isAbsolute()) {
            throw new IllegalArgumentException("Storage path must be relative");
        }

        Path resolvedPath = rootDirectory.resolve(relativePath).normalize();

        if (!resolvedPath.startsWith(rootDirectory) || resolvedPath.equals(rootDirectory)) {
            throw new IllegalArgumentException("Invalid storage path");
        }

        return resolvedPath;
    }

    private String toPortableStoragePath(Path absolutePath) {
        Path relativePath = rootDirectory.relativize(absolutePath);

        return StreamSupport.stream(relativePath.spliterator(), false)
                .map(Path::toString)
                .reduce((left, right) -> left + "/" + right)
                .orElseThrow(() -> new IllegalStateException("Stored file path could not be resolved"));
    }

    private void initialiseRootDirectory() {
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialise storage directory", exception);
        }
    }
}