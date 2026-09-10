package net.imaginethinking.appointmentpack.pack;

import org.springframework.core.io.Resource;

/**
 * Keeps the generated Appointment Pack file together with the information needed to return it.
 */
public record AppointmentPackFile(
        Resource resource,
        String fileName,
        String contentType,
        long fileSize
) {
}