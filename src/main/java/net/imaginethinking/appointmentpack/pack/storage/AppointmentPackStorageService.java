package net.imaginethinking.appointmentpack.pack.storage;

import net.imaginethinking.appointmentpack.common.storage.FileSystemStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AppointmentPackStorageService {

    private final FileSystemStorage fileSystemStorage;

    public AppointmentPackStorageService(
            @Value("${appointment-pack.appointment-packs.storage-directory}")
            String storageDirectory) {
        fileSystemStorage = new FileSystemStorage(storageDirectory);
    }

    public String store(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("Appointment pack PDF must not be empty");
        }

        String storedFileName = UUID.randomUUID() + ".pdf";
        String storagePath = createStoragePath(storedFileName);

        return fileSystemStorage.store(pdfBytes, storagePath);
    }

    public Resource load(String storagePath) {
        return fileSystemStorage.load(storagePath);
    }

    public void delete(String storagePath) {
        fileSystemStorage.delete(storagePath);
    }

    private String createStoragePath(String storedFileName) {
        String directoryName = storedFileName.substring(0, 2);

        return directoryName + "/" + storedFileName;
    }
}