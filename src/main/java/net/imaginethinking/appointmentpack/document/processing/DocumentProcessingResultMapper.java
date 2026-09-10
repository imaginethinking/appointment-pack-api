package net.imaginethinking.appointmentpack.document.processing;

import net.imaginethinking.appointmentpack.document.Document;
import net.imaginethinking.appointmentpack.document.DocumentType;
import net.imaginethinking.appointmentpack.document.processing.api.AppointmentDetailsResponse;
import net.imaginethinking.appointmentpack.document.processing.api.DocumentProcessingResultResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Maps saved processing results into the response used by document review pages.
 */
@Component
public class DocumentProcessingResultMapper {

    /**
     * Builds the response from the supplied document.
     */
    public DocumentProcessingResultResponse toResponse(Document document) {
        return new DocumentProcessingResultResponse(
                document.getId(),
                document.getDocumentType(),
                document.getStatus(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Builds the response from the supplied document.
     */
    public DocumentProcessingResultResponse toResponse(
            Document document,
            DocumentProcessingResult result) {
        DocumentProcessingResultResponse.ModelMetadata model =
                result.getModelName() == null
                        ? null
                        : new DocumentProcessingResultResponse.ModelMetadata(
                        result.getModelName(),
                        result.getPromptVersion());

        UUID appointmentReviewerId = result.getAppointmentReviewedBy() == null
                ? null
                : result.getAppointmentReviewedBy().getId();

        UUID deidentificationReviewerId = result.getDeidentificationReviewedBy() == null
                ? null
                : result.getDeidentificationReviewedBy().getId();

        UUID summaryReviewerId = result.getSummaryReviewedBy() == null
                ? null
                : result.getSummaryReviewedBy().getId();

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
                result.getSummaryReviewedAt()
        );
    }

    /**
     * Builds the appointment details from the supplied document.
     */
    private AppointmentDetailsResponse toAppointmentDetails(
            Document document,
            DocumentProcessingResult result) {
        if (document.getDocumentType()
                != DocumentType.APPOINTMENT_LETTER) {
            return null;
        }

        return new AppointmentDetailsResponse(
                result.getAppointmentDate(),
                result.getAppointmentStartTime(),
                result.getAppointmentEndTime(),
                result.getAppointmentService(),
                result.getAppointmentType(),
                result.getAppointmentClinicianOrTeam(),
                result.getAppointmentLocationName(),
                toAppointmentAddress(result)
        );
    }

    /**
     * Builds the appointment address from the supplied document processing result.
     */
    private AppointmentDetailsResponse.AddressDetails
    toAppointmentAddress(
            DocumentProcessingResult result) {
        boolean empty = isBlank(result.getAppointmentAddressLine1())
                && isBlank(result.getAppointmentAddressLine2())
                && isBlank(result.getAppointmentTownCity())
                && isBlank(result.getAppointmentCounty())
                && isBlank(result.getAppointmentPostcode())
                && isBlank(result.getAppointmentCountry());

        if (empty) {
            return null;
        }

        return new AppointmentDetailsResponse.AddressDetails(
                result.getAppointmentAddressLine1(),
                result.getAppointmentAddressLine2(),
                result.getAppointmentTownCity(),
                result.getAppointmentCounty(),
                result.getAppointmentPostcode(),
                result.getAppointmentCountry()
        );
    }

    /**
     * Checks whether a text value is null or contains only whitespace.
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}