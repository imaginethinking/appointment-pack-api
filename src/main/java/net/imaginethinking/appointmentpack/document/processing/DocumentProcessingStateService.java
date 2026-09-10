package net.imaginethinking.appointmentpack.document.processing;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.document.*;
import net.imaginethinking.appointmentpack.document.processing.api.AppointmentDetailsResponse;
import net.imaginethinking.appointmentpack.document.processing.api.DocumentProcessingResultResponse;
import net.imaginethinking.appointmentpack.document.processing.client.DocumentExtractionResponse;
import net.imaginethinking.appointmentpack.document.processing.client.DocumentProcessingException;
import net.imaginethinking.appointmentpack.document.processing.client.DocumentSummaryResponse;
import net.imaginethinking.appointmentpack.document.processing.context.DocumentExtractionContext;
import net.imaginethinking.appointmentpack.document.processing.context.DocumentSummarisationContext;
import net.imaginethinking.appointmentpack.document.processing.context.RedactionContext;
import net.imaginethinking.appointmentpack.document.processing.context.RedactionContextFactory;
import net.imaginethinking.appointmentpack.event.AppEventPublisher;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityAction;
import net.imaginethinking.appointmentpack.event.patient.PatientActivityEvent;
import net.imaginethinking.appointmentpack.event.patient.PatientResourceType;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import net.imaginethinking.appointmentpack.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

/**
 * Applies the document processing state changes before and after extraction, review and summarisation.
 */
@Service
@RequiredArgsConstructor
public class DocumentProcessingStateService {

    private final DocumentRepository documentRepository;
    private final DocumentProcessingResultRepository processingResultRepository;
    private final RedactionContextFactory redactionContextFactory;
    private final DocumentProcessingRecordService processingRecordService;
    private final PatientRecordAccessService patientRecordAccessService;
    private final DocumentProcessingResultMapper processingResultMapper;
    private final EntityManager entityManager;
    private final AppEventPublisher appEventPublisher;
    private final DocumentProcessingLifecyclePolicy lifecyclePolicy;

    /**
     * Loads the current processing result after checking that the document can be viewed.
     */
    @Transactional(readOnly = true)
    public DocumentProcessingResultResponse getProcessing(UUID authenticatedUserId, UUID documentId) {
        Document document = processingRecordService.requireAvailableDocument(documentId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.VIEW);

        return processingRecordService.findProcessingResult(documentId)
                .map(result -> processingResultMapper.toResponse(document, result))
                .orElseGet(() -> processingResultMapper.toResponse(document));
    }

    /**
     * Checks that extraction can start, moves the document into the extracting state and returns the values needed
     * for processing.
     */
    @Transactional
    public DocumentExtractionContext beginExtraction(UUID authenticatedUserId, UUID documentId) {
        Document document = processingRecordService.requireAvailableDocument(documentId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.EDIT);

        recoverStaleProcessingState(document);
        validateExtractionCanBegin(document);

        RedactionContext redactionContext = document.getDocumentType() == DocumentType.CONSULTATION_OUTCOME_LETTER
                ? redactionContextFactory.create(document)
                : RedactionContext.empty();

        document.setStatus(DocumentStatus.EXTRACTING);
        document.setProcessingFailureReason(null);

        return new DocumentExtractionContext(
                document.getId(),
                document.getDocumentType(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getStoragePath(),
                redactionContext);
    }

    /**
     * Checks the extraction response, saves its result and moves the document to the correct review state.
     */
    @Transactional
    public DocumentProcessingResultResponse completeExtraction(UUID documentId, DocumentExtractionResponse response) {
        Document document = processingRecordService.requireAvailableDocument(documentId);

        if (document.getStatus() != DocumentStatus.EXTRACTING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document extraction is not in progress");
        }

        validateExtractionResponse(document, response);

        DocumentProcessingResult result = saveExtractionResult(document, response);

        document.setStatus(determineReviewStatus(document.getDocumentType()));
        document.setProcessingFailureReason(null);

        return processingResultMapper.toResponse(document, result);
    }

    /**
     * Moves an extracting document into the failed state and stores the failure message.
     */
    @Transactional
    public void failExtraction(UUID documentId, String failureReason) {
        documentRepository.findById(documentId)
                .filter(document -> document.getStatus() == DocumentStatus.EXTRACTING)
                .ifPresent(document -> {
                    document.setStatus(DocumentStatus.EXTRACTION_FAILED);
                    document.setProcessingFailureReason(failureReason);
                });
    }

    /**
     * Checks the consultation review state, stores the exact approved text and moves the document into
     * summarisation.
     */
    @Transactional
    public DocumentSummarisationContext beginSummarisation(
            UUID authenticatedUserId,
            UUID documentId,
            String approvedDeidentifiedText) {
        Document document = processingRecordService.requireAvailableDocument(documentId);

        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.EDIT);

        if (document.getDocumentType() != DocumentType.CONSULTATION_OUTCOME_LETTER) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only consultation outcome letters require external summarisation");
        }

        recoverStaleProcessingState(document);

        DocumentProcessingResult result = processingRecordService.requireProcessingResult(documentId);

        // The first request records the reviewed text. A retry must keep using that exact approved version.
        if (document.getStatus() == DocumentStatus.READY_FOR_DEIDENTIFICATION_REVIEW) {
            approveDeidentifiedText(result, authenticatedUserId, approvedDeidentifiedText);

            appEventPublisher.publish(PatientActivityEvent.create(
                    authenticatedUserId,
                    document.getPatientRecord().getId(),
                    PatientResourceType.DOCUMENT,
                    document.getId(),
                    PatientActivityAction.DEIDENTIFICATION_APPROVED));
        } else if (document.getStatus() == DocumentStatus.SUMMARISATION_FAILED) {
            validateRetrySnapshot(result, approvedDeidentifiedText);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Document cannot be summarised in its current status");
        }

        document.setStatus(DocumentStatus.SUMMARISING);
        document.setProcessingFailureReason(null);

        return new DocumentSummarisationContext(
                document.getId(),
                document.getDocumentType(),
                result.getApprovedDeidentifiedText());
    }

    /**
     * Checks the summary response, saves the generated summary and moves the document to summary review.
     */
    @Transactional
    public DocumentProcessingResultResponse completeSummarisation(UUID documentId, DocumentSummaryResponse response) {
        Document document = processingRecordService.requireAvailableDocument(documentId);

        if (document.getStatus() != DocumentStatus.SUMMARISING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document summarisation is not in progress");
        }

        validateSummaryResponse(document, response);

        DocumentProcessingResult result = processingRecordService.requireProcessingResult(documentId);

        result.setGeneratedSummary(response.summary());
        result.setReviewedSummary(null);
        result.setSummarySource(SummarySource.OPENAI);
        result.setProcessorVersion(response.processorVersion());
        result.setModelName(response.modelName());
        result.setPromptVersion(response.promptVersion());
        result.setSummaryReviewedBy(null);
        result.setSummaryReviewedAt(null);

        document.setStatus(DocumentStatus.READY_FOR_SUMMARY_REVIEW);
        document.setProcessingFailureReason(null);

        return processingResultMapper.toResponse(document, result);
    }

    /**
     * Moves a summarising document into the failed state while keeping its previously approved text for retry.
     */
    @Transactional
    public void failSummarisation(UUID documentId, String failureReason) {
        documentRepository.findById(documentId)
                .filter(document -> document.getStatus() == DocumentStatus.SUMMARISING)
                .ifPresent(document -> {
                    document.setStatus(DocumentStatus.SUMMARISATION_FAILED);
                    document.setProcessingFailureReason(failureReason);
                });
    }

    /**
     * Stores the exact deidentified text approved by the user together with the reviewer and approval time.
     */
    private void approveDeidentifiedText(
            DocumentProcessingResult result,
            UUID authenticatedUserId,
            String approvedDeidentifiedText) {
        if (isBlank(approvedDeidentifiedText)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approved de-identified text is required");
        }

        result.setApprovedDeidentifiedText(approvedDeidentifiedText);
        result.setDeidentificationReviewedBy(entityManager.getReference(User.class, authenticatedUserId));
        result.setDeidentificationReviewedAt(Instant.now());
    }

    /**
     * Checks that a retry uses exactly the same deidentified text that was approved earlier.
     */
    private void validateRetrySnapshot(DocumentProcessingResult result, String approvedDeidentifiedText) {
        if (result.getApprovedDeidentifiedText() == null || !result.getApprovedDeidentifiedText()
                .equals(approvedDeidentifiedText)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The approved de-identified text cannot be changed during retry");
        }
    }

    /**
     * Stores the extracted text, deidentified text and processing details returned by extraction.
     */
    private DocumentProcessingResult saveExtractionResult(Document document, DocumentExtractionResponse response) {
        DocumentProcessingResult result = processingResultRepository.findByDocumentId(document.getId())
                .orElseGet(DocumentProcessingResult::new);

        result.setDocument(document);
        result.setExtractedText(response.extractedText());
        result.setProcessingWarning(response.processingWarning());
        result.setProcessorVersion(response.processorVersion());

        // A fresh extraction replaces the previous review output so old approval or summary data does not carry
        // into the new result.
        result.setApprovedDeidentifiedText(null);
        result.setGeneratedSummary(null);
        result.setReviewedSummary(null);
        result.setSummarySource(null);
        result.setModelName(null);
        result.setPromptVersion(null);

        result.setAppointmentReviewedBy(null);
        result.setAppointmentReviewedAt(null);
        result.setDeidentificationReviewedBy(null);
        result.setDeidentificationReviewedAt(null);
        result.setSummaryReviewedBy(null);
        result.setSummaryReviewedAt(null);

        if (document.getDocumentType() == DocumentType.APPOINTMENT_LETTER) {
            result.setMachineDeidentifiedText(null);
            applyAppointmentDetails(result, response.appointmentDetails());
        } else {
            clearAppointmentDetails(result);
            result.setMachineDeidentifiedText(response.deidentifiedText());
        }

        return processingResultRepository.save(result);
    }

    /**
     * Copies the extracted appointment suggestions into the saved processing result.
     */
    private void applyAppointmentDetails(DocumentProcessingResult result, AppointmentDetailsResponse details) {
        result.setAppointmentDate(details.date());
        result.setAppointmentStartTime(details.startTime());
        result.setAppointmentEndTime(details.endTime());
        result.setAppointmentService(details.service());
        result.setAppointmentType(details.appointmentType());
        result.setAppointmentClinicianOrTeam(details.clinicianOrTeam());
        result.setAppointmentLocationName(details.locationName());

        AppointmentDetailsResponse.AddressDetails address = details.address();

        if (address == null) {
            clearAppointmentAddress(result);
            return;
        }

        result.setAppointmentAddressLine1(address.addressLine1());
        result.setAppointmentAddressLine2(address.addressLine2());
        result.setAppointmentTownCity(address.townCity());
        result.setAppointmentCounty(address.county());
        result.setAppointmentPostcode(address.postcode());
        result.setAppointmentCountry(address.country());
    }

    /**
     * Clears appointment suggestions when the current processing result is not an appointment letter.
     */
    private void clearAppointmentDetails(DocumentProcessingResult result) {
        result.setAppointmentDate(null);
        result.setAppointmentStartTime(null);
        result.setAppointmentEndTime(null);
        result.setAppointmentService(null);
        result.setAppointmentType(null);
        result.setAppointmentClinicianOrTeam(null);
        result.setAppointmentLocationName(null);

        clearAppointmentAddress(result);
    }

    /**
     * Clears the saved appointment address before applying a new extracted address.
     */
    private void clearAppointmentAddress(DocumentProcessingResult result) {
        result.setAppointmentAddressLine1(null);
        result.setAppointmentAddressLine2(null);
        result.setAppointmentTownCity(null);
        result.setAppointmentCounty(null);
        result.setAppointmentPostcode(null);
        result.setAppointmentCountry(null);
    }

    /**
     * Moves a document out of a stale processing state and records the recovery reason.
     */
    private void recoverStaleProcessingState(Document document) {
        Instant now = Instant.now();

        if (!lifecyclePolicy.isStale(document, now)) {
            return;
        }

        if (document.getStatus() == DocumentStatus.EXTRACTING) {
            document.setStatus(DocumentStatus.EXTRACTION_FAILED);
            document.setProcessingFailureReason(DocumentProcessingLifecyclePolicy.STALE_EXTRACTION_FAILURE_REASON);
            return;
        }

        if (document.getStatus() == DocumentStatus.SUMMARISING) {
            document.setStatus(DocumentStatus.SUMMARISATION_FAILED);
            document.setProcessingFailureReason(DocumentProcessingLifecyclePolicy.STALE_SUMMARISATION_FAILURE_REASON);
        }
    }

    /**
     * Checks the document type and current state before allowing extraction to start or be retried.
     */
    private void validateExtractionCanBegin(Document document) {
        boolean extractable = document.getStatus() == DocumentStatus.UPLOADED || document.getStatus() == DocumentStatus.EXTRACTION_FAILED;

        if (!extractable) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Document cannot be extracted in its current status");
        }
    }

    /**
     * Checks that the extraction response belongs to the requested document and contains the expected result for
     * its type.
     */
    private void validateExtractionResponse(Document document, DocumentExtractionResponse response) {
        if (response == null) {
            throw new DocumentProcessingException("Document extraction service returned an empty response");
        }

        if (!document.getId().equals(response.documentId())) {
            throw new DocumentProcessingException("Document extraction response contains an unexpected document ID");
        }

        if (isBlank(response.extractedText())) {
            throw new DocumentProcessingException("Document extraction response contains no extracted text");
        }

        if (isBlank(response.processorVersion())) {
            throw new DocumentProcessingException("Document extraction response contains no processor version");
        }

        if (document.getDocumentType() == DocumentType.APPOINTMENT_LETTER && response.appointmentDetails() == null) {
            throw new DocumentProcessingException("Appointment extraction response contains no appointment details");
        }

        if (document.getDocumentType() == DocumentType.CONSULTATION_OUTCOME_LETTER && isBlank(response.deidentifiedText())) {
            throw new DocumentProcessingException("Consultation extraction response contains no de-identified text");
        }
    }

    /**
     * Checks that the summary response belongs to the document and contains a usable generated summary.
     */
    private void validateSummaryResponse(Document document, DocumentSummaryResponse response) {
        if (response == null) {
            throw new DocumentProcessingException("Document summarisation service returned an empty response");
        }

        if (!document.getId().equals(response.documentId())) {
            throw new DocumentProcessingException("Document summarisation response contains an unexpected document ID");
        }

        if (isBlank(response.summary()) || isBlank(response.processorVersion()) || isBlank(response.modelName()) || isBlank(
                response.promptVersion())) {
            throw new DocumentProcessingException("Document summarisation response is incomplete");
        }
    }

    /**
     * Chooses the review state that follows extraction from the document type.
     */
    private DocumentStatus determineReviewStatus(DocumentType documentType) {
        return switch (documentType) {
            case APPOINTMENT_LETTER -> DocumentStatus.READY_FOR_APPOINTMENT_REVIEW;

            case CONSULTATION_OUTCOME_LETTER -> DocumentStatus.READY_FOR_DEIDENTIFICATION_REVIEW;
        };
    }

    /**
     * Checks whether a text value is null or contains only whitespace.
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}