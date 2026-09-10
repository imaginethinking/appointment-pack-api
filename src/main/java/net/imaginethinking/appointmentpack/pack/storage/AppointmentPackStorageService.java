package net.imaginethinking.appointmentpack.pack.storage;

import net.imaginethinking.appointmentpack.common.storage.FileSystemStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Stores generated Appointment Pack PDFs underneath the configured pack storage directory.
 */
@Service
public class AppointmentPackStorageService {

    private final FileSystemStorage fileSystemStorage;

    /**
     * Creates the Appointment Pack storage helper using the configured storage directory.
     */
    public AppointmentPackStorageService(
            @Value("${appointment-pack.appointment-packs.storage-directory}")
            String storageDirectory) {
        fileSystemStorage = new FileSystemStorage(storageDirectory);
    }

    /**
     * Writes the generated PDF using a random file name and returns its saved relative path.
     */
    public String store(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("Appointment pack PDF must not be empty");
        }

        String storedFileName = UUID.randomUUID() + ".pdf";
        String storagePath = createStoragePath(storedFileName);

        return fileSystemStorage.store(pdfBytes, storagePath);
    }

    /**
     * Loads a generated Appointment Pack PDF from its saved relative path.
     */
    public Resource load(String storagePath) {
        return fileSystemStorage.load(storagePath);
    }

    /**
     * Removes a generated Appointment Pack PDF when cleanup is required.
     */
    public void delete(String storagePath) {
        fileSystemStorage.delete(storagePath);
    }

    /**
     * Places the generated PDF file name underneath the Appointment Pack storage folder.
     */
    private String createStoragePath(String storedFileName) {
        String directoryName = storedFileName.substring(0, 2);

        return directoryName + "/" + storedFileName;
    }
}