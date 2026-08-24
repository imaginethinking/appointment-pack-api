package net.imaginethinking.appointmentpack.pack;

import org.springframework.core.io.Resource;

public record AppointmentPackFile(
        Resource resource,
        String fileName,
        String contentType,
        long fileSize
) {
}