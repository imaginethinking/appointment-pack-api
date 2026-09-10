package net.imaginethinking.appointmentpack.document;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.appointment.AppointmentConfirmationRequest;
import net.imaginethinking.appointmentpack.appointment.AppointmentResponse;
import net.imaginethinking.appointmentpack.document.processing.api.DocumentProcessingResultResponse;
import net.imaginethinking.appointmentpack.document.processing.DocumentProcessingService;
import net.imaginethinking.appointmentpack.document.processing.api.DocumentSummarisationRequest;
import net.imaginethinking.appointmentpack.document.processing.api.DocumentSummaryAcceptanceRequest;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Handles document upload, processing, review, download and archive requests.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class DocumentController {
    private final DocumentService documentService;
    private final DocumentProcessingService documentProcessingService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    /**
     * Uploads a document for the selected patient and returns the newly created document record.
     */
    @PostMapping(
            path = "/patient-records/{patientRecordId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentResponse> uploadDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId,
            @RequestParam DocumentType documentType,
            @RequestParam MultipartFile file) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        DocumentResponse response = documentService.upload(userId, patientRecordId, documentType, file);

        return ResponseEntity.created(URI.create("/api/v1/documents/" + response.id())).body(response);
    }

    /**
     * Returns the active documents for the selected patient.
     */
    @GetMapping("/patient-records/{patientRecordId}/documents")
    public ResponseEntity<List<DocumentResponse>> getDocuments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        List<DocumentResponse> response = documentService.getDocuments(userId, patientRecordId);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns the requested document when the signed in user can view it.
     */
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<DocumentResponse> getDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID documentId) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        DocumentResponse response = documentService.getDocument(userId, documentId);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns the current extraction, review and summary information for the requested document.
     */
    @GetMapping("/documents/{documentId}/processing")
    public ResponseEntity<DocumentProcessingResultResponse> getDocumentProcessing(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID documentId) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        DocumentProcessingResultResponse response = documentProcessingService.getProcessing(userId, documentId);

        return ResponseEntity.ok(response);
    }

    /**
     * Starts consultation summarisation using the exact deidentified text submitted after review.
     */
    @PostMapping("/documents/{documentId}/summarise")
    public ResponseEntity<DocumentProcessingResultResponse> summariseDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID documentId,
            @Valid @RequestBody DocumentSummarisationRequest request) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        DocumentProcessingResultResponse response = documentProcessingService.summarise(
                userId,
                documentId,
                request.approvedDeidentifiedText());

        return ResponseEntity.ok(response);
    }

    /**
     * Accepts the reviewed consultation summary into Medical History.
     */
    @PostMapping("/documents/{documentId}/summary/accept")
    public ResponseEntity<DocumentProcessingResultResponse> acceptDocumentSummary(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID documentId,
            @Valid @RequestBody DocumentSummaryAcceptanceRequest request) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        DocumentProcessingResultResponse response = documentProcessingService.acceptSummary(
                userId,
                documentId,
                request);

        return ResponseEntity.ok(response);
    }

    /**
     * Rejects the generated consultation summary for the requested document.
     */
    @PostMapping("/documents/{documentId}/summary/reject")
    public ResponseEntity<DocumentProcessingResultResponse> rejectDocumentSummary(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID documentId) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        DocumentProcessingResultResponse response = documentProcessingService.rejectSummary(userId, documentId);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns the original uploaded document as an attachment with its saved content type and file name.
     */
    @GetMapping("/documents/{documentId}/file")
    public ResponseEntity<Resource> downloadDocument(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID documentId) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        DocumentDownload download = documentService.download(userId, documentId);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.resource());
    }

    /**
     * Starts extraction for the requested document and returns its updated processing state.
     */
    @PostMapping("/documents/{documentId}/extract")
    public ResponseEntity<DocumentProcessingResultResponse> extractDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID documentId) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        DocumentProcessingResultResponse response = documentProcessingService.extract(userId, documentId);

        return ResponseEntity.ok(response);
    }

    /**
     * Confirms the reviewed appointment details and returns the appointment created from the document.
     */
    @PostMapping("/documents/{documentId}/appointment/confirm")
    public ResponseEntity<AppointmentResponse> confirmAppointment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID documentId,
            @Valid @RequestBody AppointmentConfirmationRequest request) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        AppointmentResponse response = documentProcessingService.confirmAppointment(userId, documentId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Rejects the appointment suggestions produced from the requested document.
     */
    @PostMapping("/documents/{documentId}/appointment/reject")
    public ResponseEntity<DocumentProcessingResultResponse> rejectAppointment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID documentId) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        DocumentProcessingResultResponse response = documentProcessingService.rejectAppointment(userId, documentId);

        return ResponseEntity.ok(response);
    }

    /**
     * Archives the requested document and returns its updated details.
     */
    @PatchMapping("/documents/{documentId}/archive")
    public ResponseEntity<DocumentResponse> archiveDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID documentId) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        DocumentResponse response = documentService.archive(userId, documentId);

        return ResponseEntity.ok(response);
    }
}
