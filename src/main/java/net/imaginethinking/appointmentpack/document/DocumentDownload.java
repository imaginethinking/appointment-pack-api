package net.imaginethinking.appointmentpack.document;

import org.springframework.core.io.Resource;

public record DocumentDownload(
        Resource resource,
        String fileName,
        String contentType,
        long fileSize
) {
}
