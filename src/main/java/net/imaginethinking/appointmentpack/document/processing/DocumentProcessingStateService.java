package net.imaginethinking.appointmentpack.document.processing;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.address.Address;
import net.imaginethinking.appointmentpack.appointment.*;
import net.imaginethinking.appointmentpack.document.*;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryEntry;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryEntryRepository;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistoryPermission;
import net.imaginethinking.appointmentpack.medicalhistory.MedicalHistorySourceType;
import net.imaginethinking.appointmentpack.patientcareraccess.PatientAccessControlService;
import net.imaginethinking.appointmentpack.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessingStateService {

    private final DocumentRepository documentRepository;

    private final DocumentProcessingResultRepository processingResultRepository;

    private final MedicalHistoryEntryRepository medicalHistoryEntryRepository;

    private final AppointmentRepository appointmentRepository;

    private final RedactionContextFactory redactionContextFactory;

    private final PatientAccessControlService patientAccessControlService;

    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public DocumentProcessingResultResponse getProcessing(UUID authenticatedUserId, UUID documentId) {
        Document document = findAvailableDocument(documentId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.VIEW);

        DocumentProcessingResult result = findProcessingResult(documentId);

        return toResponse(document, result);
    }

    @Transactional
    public DocumentExtractionContext beginExtraction(UUID authenticatedUserId, UUID documentId) {
        Document document = findAvailableDocument(documentId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.EDIT);

        validateExtractionCanBegin(document);

        RedactionContext redactionContext = redactionContextFactory.create(document);

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

    @Transactional
    public DocumentProcessingResultResponse completeExtraction(UUID documentId, DocumentExtractionResponse response) {
        Document document = findAvailableDocument(documentId);

        if (document.getStatus() != DocumentStatus.EXTRACTING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document extraction is not in progress");
        }

        validateExtractionResponse(document, response);

        DocumentProcessingResult result = saveExtractionResult(document, response);

        document.setStatus(determineReviewStatus(document.getDocumentType()));

        document.setProcessingFailureReason(null);

        return toResponse(document, result);
    }

    @Transactional
    public void failExtraction(UUID documentId, String failureReason) {
        documentRepository.findById(documentId)
                .filter(document -> document.getStatus() == DocumentStatus.EXTRACTING)
                .ifPresent(document -> {
                    document.setStatus(DocumentStatus.EXTRACTION_FAILED);

                    document.setProcessingFailureReason(failureReason);
                });
    }

    @Transactional
    public DocumentSummarisationContext beginSummarisation(
            UUID authenticatedUserId,
            UUID documentId,
            String approvedDeidentifiedText) {
        Document document = findAvailableDocument(documentId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.EDIT);

        if (document.getDocumentType() != DocumentType.CONSULTATION_OUTCOME_LETTER) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only consultation outcome letters require external summarisation");
        }

        DocumentProcessingResult result = findProcessingResult(documentId);

        if (document.getStatus() == DocumentStatus.READY_FOR_DEIDENTIFICATION_REVIEW) {
            approveDeidentifiedText(result, authenticatedUserId, approvedDeidentifiedText);
        } else if (document.getStatus() == DocumentStatus.SUMMARISATION_FAILED) {
            validateRetrySnapshot(result, approvedDeidentifiedText);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Document cannot be summarised in its current status");
        }

        document.setStatus(DocumentStatus.SUMMARISING);

        document.setProcessingFailureReason(null);

        return new DocumentSummarisationContext(document.getId(), result.getApprovedDeidentifiedText());
    }

    @Transactional
    public DocumentProcessingResultResponse completeSummarisation(UUID documentId, DocumentSummaryResponse response) {
        Document document = findAvailableDocument(documentId);

        if (document.getStatus() != DocumentStatus.SUMMARISING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document summarisation is not in progress");
        }

        validateSummaryResponse(document, response);

        DocumentProcessingResult result = findProcessingResult(documentId);

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

        return toResponse(document, result);
    }

    @Transactional
    public void failSummarisation(UUID documentId, String failureReason) {
        documentRepository.findById(documentId)
                .filter(document -> document.getStatus() == DocumentStatus.SUMMARISING)
                .ifPresent(document -> {
                    document.setStatus(DocumentStatus.SUMMARISATION_FAILED);

                    document.setProcessingFailureReason(failureReason);
                });
    }

    @Transactional
    public AppointmentResponse confirmAppointment(
            UUID authenticatedUserId,
            UUID documentId,
            AppointmentConfirmationRequest request) {
        Document document = findAvailableDocument(documentId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.EDIT);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                document.getPatientRecord(),
                AppointmentPermission.EDIT);

        validateAppointmentCanBeConfirmed(document);

        if (appointmentRepository.existsBySourceDocument_Id(documentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An appointment already exists for this document");
        }

        validateAppointmentTimes(request);

        DocumentProcessingResult result = findProcessingResult(documentId);

        User reviewingUser = entityManager.getReference(User.class, authenticatedUserId);

        result.setAppointmentReviewedBy(reviewingUser);
        result.setAppointmentReviewedAt(Instant.now());

        Appointment appointment = new Appointment();

        appointment.setPatientRecord(document.getPatientRecord());

        appointment.setDate(request.date());
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(request.endTime());
        appointment.setService(normaliseOptionalValue(request.service()));
        appointment.setAppointmentType(normaliseOptionalValue(request.appointmentType()));
        appointment.setClinicianOrTeam(normaliseOptionalValue(request.clinicianOrTeam()));
        appointment.setLocationName(normaliseOptionalValue(request.locationName()));
        appointment.setAddress(toAddress(request.address()));
        appointment.setNotes(normaliseOptionalValue(request.notes()));
        appointment.setSourceDocument(document);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        document.setStatus(DocumentStatus.ACCEPTED);
        document.setProcessingFailureReason(null);

        return AppointmentResponse.from(savedAppointment);
    }

    @Transactional
    public DocumentProcessingResultResponse rejectAppointment(UUID authenticatedUserId, UUID documentId) {
        Document document = findAvailableDocument(documentId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.EDIT);

        if (document.getDocumentType() != DocumentType.APPOINTMENT_LETTER || document.getStatus() != DocumentStatus.READY_FOR_APPOINTMENT_REVIEW) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Appointment details cannot be rejected in the current document state");
        }

        DocumentProcessingResult result = findProcessingResult(documentId);

        result.setAppointmentReviewedBy(entityManager.getReference(User.class, authenticatedUserId));
        result.setAppointmentReviewedAt(Instant.now());
        document.setStatus(DocumentStatus.REJECTED);
        document.setProcessingFailureReason(null);

        return toResponse(document, result);
    }

    @Transactional
    public DocumentProcessingResultResponse acceptSummary(
            UUID authenticatedUserId,
            UUID documentId,
            DocumentSummaryAcceptanceRequest request) {
        Document document = findAvailableDocument(documentId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.EDIT);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                document.getPatientRecord(),
                MedicalHistoryPermission.EDIT);

        validateSummaryCanBeAccepted(document);

        if (medicalHistoryEntryRepository.existsBySourceDocumentId(documentId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A medical-history entry already exists for this document");
        }

        DocumentProcessingResult result = findProcessingResult(documentId);

        if (document.getStatus() == DocumentStatus.READY_FOR_SUMMARY_REVIEW) {
            validateGeneratedSummary(result);
        } else {
            applyManualSummary(result);
        }

        String reviewedSummary = request.reviewedSummary().strip();

        User reviewingUser = entityManager.getReference(User.class, authenticatedUserId);

        Instant reviewedAt = Instant.now();

        result.setReviewedSummary(reviewedSummary);
        result.setSummaryReviewedBy(reviewingUser);
        result.setSummaryReviewedAt(reviewedAt);

        MedicalHistoryEntry historyEntry = new MedicalHistoryEntry();

        historyEntry.setPatientRecord(document.getPatientRecord());

        historyEntry.setTitle(request.historyTitle().strip());

        historyEntry.setSummary(reviewedSummary);
        historyEntry.setEntryDate(request.historyDate());
        historyEntry.setSourceType(MedicalHistorySourceType.DOCUMENT_SUMMARY);
        historyEntry.setSourceDocument(document);
        historyEntry.setCreatedBy(reviewingUser);

        medicalHistoryEntryRepository.save(historyEntry);

        document.setStatus(DocumentStatus.ACCEPTED);
        document.setProcessingFailureReason(null);

        return toResponse(document, result);
    }

    @Transactional
    public DocumentProcessingResultResponse rejectSummary(UUID authenticatedUserId, UUID documentId) {
        Document document = findAvailableDocument(documentId);

        patientAccessControlService.requirePermission(
                authenticatedUserId,
                document.getPatientRecord(),
                DocumentPermission.EDIT);

        if (document.getDocumentType() != DocumentType.CONSULTATION_OUTCOME_LETTER || document.getStatus() != DocumentStatus.READY_FOR_SUMMARY_REVIEW) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Document summary cannot be rejected in its current status");
        }

        DocumentProcessingResult result = findProcessingResult(documentId);

        result.setReviewedSummary(null);
        result.setSummaryReviewedBy(entityManager.getReference(User.class, authenticatedUserId));
        result.setSummaryReviewedAt(Instant.now());

        document.setStatus(DocumentStatus.REJECTED);
        document.setProcessingFailureReason(null);

        return toResponse(document, result);
    }

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

    private void validateRetrySnapshot(DocumentProcessingResult result, String approvedDeidentifiedText) {
        if (result.getApprovedDeidentifiedText() == null || !result.getApprovedDeidentifiedText()
                .equals(approvedDeidentifiedText)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The approved de-identified text cannot be changed during retry");
        }
    }

    private void validateAppointmentCanBeConfirmed(Document document) {
        if (document.getDocumentType() != DocumentType.APPOINTMENT_LETTER || document.getStatus() != DocumentStatus.READY_FOR_APPOINTMENT_REVIEW) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Appointment cannot be confirmed in the current document state");
        }
    }

    private void validateAppointmentTimes(AppointmentConfirmationRequest request) {
        if (request.endTime() != null && !request.endTime().isAfter(request.startTime())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Appointment end time must be after the start time");
        }
    }

    private void validateSummaryCanBeAccepted(Document document) {
        if (document.getDocumentType() != DocumentType.CONSULTATION_OUTCOME_LETTER) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only consultation outcome summaries can be accepted into medical history");
        }

        boolean acceptable = document.getStatus() == DocumentStatus.READY_FOR_SUMMARY_REVIEW || document.getStatus() == DocumentStatus.SUMMARISATION_FAILED;

        if (!acceptable) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Document summary cannot be accepted in its current status");
        }
    }

    private void validateGeneratedSummary(DocumentProcessingResult result) {
        if (isBlank(result.getGeneratedSummary()) || result.getSummarySource() == null) {
            throw new DocumentProcessingException("Document processing result contains no generated summary");
        }
    }

    private void applyManualSummary(DocumentProcessingResult result) {
        result.setGeneratedSummary(null);
        result.setSummarySource(SummarySource.MANUAL);
        result.setModelName(null);
        result.setPromptVersion(null);
    }

    private DocumentProcessingResult saveExtractionResult(Document document, DocumentExtractionResponse response) {
        DocumentProcessingResult result = processingResultRepository.findByDocumentId(document.getId())
                .orElseGet(DocumentProcessingResult::new);

        result.setDocument(document);
        result.setExtractedText(response.extractedText());

        result.setProcessingWarning(response.processingWarning());
        result.setProcessorVersion(response.processorVersion());

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

    private void clearAppointmentAddress(DocumentProcessingResult result) {
        result.setAppointmentAddressLine1(null);
        result.setAppointmentAddressLine2(null);
        result.setAppointmentTownCity(null);
        result.setAppointmentCounty(null);
        result.setAppointmentPostcode(null);
        result.setAppointmentCountry(null);
    }

    private void validateExtractionCanBegin(Document document) {
        boolean extractable = document.getStatus() == DocumentStatus.UPLOADED || document.getStatus() == DocumentStatus.EXTRACTION_FAILED;

        if (!extractable) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Document cannot be extracted in its current status");
        }
    }

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

    private DocumentStatus determineReviewStatus(DocumentType documentType) {
        return switch (documentType) {
            case APPOINTMENT_LETTER -> DocumentStatus.READY_FOR_APPOINTMENT_REVIEW;

            case CONSULTATION_OUTCOME_LETTER -> DocumentStatus.READY_FOR_DEIDENTIFICATION_REVIEW;
        };
    }

    private AppointmentDetailsResponse toAppointmentDetails(Document document, DocumentProcessingResult result) {
        if (document.getDocumentType() != DocumentType.APPOINTMENT_LETTER) {
            return null;
        }

        AppointmentDetailsResponse.AddressDetails address = toAppointmentAddress(result);

        return new AppointmentDetailsResponse(
                result.getAppointmentDate(),
                result.getAppointmentStartTime(),
                result.getAppointmentEndTime(),
                result.getAppointmentService(),
                result.getAppointmentType(),
                result.getAppointmentClinicianOrTeam(),
                result.getAppointmentLocationName(),
                address);
    }

    private AppointmentDetailsResponse.AddressDetails toAppointmentAddress(
            DocumentProcessingResult result) {
        boolean empty = isBlank(result.getAppointmentAddressLine1()) && isBlank(result.getAppointmentAddressLine2()) && isBlank(
                result.getAppointmentTownCity()) && isBlank(result.getAppointmentCounty()) && isBlank(result.getAppointmentPostcode()) && isBlank(
                result.getAppointmentCountry());

        if (empty) {
            return null;
        }

        return new AppointmentDetailsResponse.AddressDetails(
                result.getAppointmentAddressLine1(),
                result.getAppointmentAddressLine2(),
                result.getAppointmentTownCity(),
                result.getAppointmentCounty(),
                result.getAppointmentPostcode(),
                result.getAppointmentCountry());
    }

    private Address toAddress(AppointmentConfirmationRequest.AddressInput request) {
        if (request == null) {
            return null;
        }

        boolean empty = isBlank(request.addressLine1()) && isBlank(request.addressLine2()) && isBlank(request.townCity()) && isBlank(
                request.county()) && isBlank(request.postcode()) && isBlank(request.country());

        if (empty) {
            return null;
        }

        Address address = new Address();

        address.setAddressLine1(normaliseOptionalValue(request.addressLine1()));
        address.setAddressLine2(normaliseOptionalValue(request.addressLine2()));
        address.setTownCity(normaliseOptionalValue(request.townCity()));
        address.setCounty(normaliseOptionalValue(request.county()));
        address.setPostcode(normaliseOptionalValue(request.postcode()));
        address.setCountry(normaliseOptionalValue(request.country()));

        return address;
    }

    private String normaliseOptionalValue(String value) {
        if (isBlank(value)) {
            return null;
        }

        return value.strip();
    }

    private DocumentProcessingResult findProcessingResult(UUID documentId) {
        return processingResultRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new DocumentProcessingException("Document processing result was not found"));
    }

    private Document findAvailableDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }

        return document;
    }

    private DocumentProcessingResultResponse toResponse(Document document, DocumentProcessingResult result) {
        DocumentProcessingResultResponse.ModelMetadata model = result.getModelName() == null ? null : new DocumentProcessingResultResponse.ModelMetadata(result.getModelName(),
                result.getPromptVersion());

        UUID appointmentReviewerId = result.getAppointmentReviewedBy() == null ? null : result.getAppointmentReviewedBy()
                .getId();

        UUID deidentificationReviewerId = result.getDeidentificationReviewedBy() == null ? null : result.getDeidentificationReviewedBy()
                .getId();

        UUID summaryReviewerId = result.getSummaryReviewedBy() == null ? null : result.getSummaryReviewedBy().getId();

        return new DocumentProcessingResultResponse(
                document.getId(),
                document.getDocumentType(),
                document.getStatus(),
                result.getExtractedText(),
                result.getMachineDeidentifiedText(),
                result.getApprovedDeidentifiedText(),
                toAppointmentDetails(document, result),
                result.getGeneratedSummary(),
                result.getReviewedSummary(),
                result.getSummarySource(),
                result.getProcessingWarning(),
                result.getProcessorVersion(),
                model,
                appointmentReviewerId,
                result.getAppointmentReviewedAt(),
                deidentificationReviewerId,
                result.getDeidentificationReviewedAt(),
                summaryReviewerId,
                result.getSummaryReviewedAt());
    }

    private boolean isBlank(
            String value) {
        return value == null || value.isBlank();
    }
}