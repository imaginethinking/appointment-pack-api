package net.imaginethinking.appointmentpack.document;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/patient-records/{patientRecordId}/documents")
public class DocumentController {
    private final DocumentService documentService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam("file") MultipartFile file
    ) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        DocumentResponse response = documentService.upload(
                userId,
                patientRecordId,
                documentType,
                file
        );

        return ResponseEntity
                .created(URI.create("/api/v1/documents/" + response.id()))
                .body(response);
    }
}
