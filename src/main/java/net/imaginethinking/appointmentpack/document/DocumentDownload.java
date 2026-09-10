package net.imaginethinking.appointmentpack.document;

import org.springframework.core.io.Resource;

/**
 * Keeps the downloaded document file together with its response details.
 */
public record DocumentDownload(
        Resource resource,
        String fileName,
        String contentType,
        long fileSize
) {
}
