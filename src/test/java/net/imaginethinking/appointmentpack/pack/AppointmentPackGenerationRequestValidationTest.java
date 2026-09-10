package net.imaginethinking.appointmentpack.pack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the validation rules used for appointment pack generation request.
 */
class AppointmentPackGenerationRequestValidationTest {

    @Test
    void shouldAcceptRepresentativeAppointmentPackRequest() {
        AppointmentPackGenerationRequest request = new AppointmentPackGenerationRequest(
                UUID.randomUUID(),
                "Appointment preparation pack",
                "Synthetic test notes",
                List.of(UUID.randomUUID()),
                List.of(UUID.randomUUID()),
                List.of(UUID.randomUUID()),
                List.of(UUID.randomUUID()),
                List.of(UUID.randomUUID()));

        assertValid(request);
    }

    @Test
    void shouldRejectNullAppointmentId() {
        AppointmentPackGenerationRequest request = new AppointmentPackGenerationRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertInvalidField(request, "appointmentId");
    }

    @Test
    void shouldEnforcePackTitleLengthBoundary() {
        assertValid(withTitle(stringOfLength(249)));
        assertValid(withTitle(stringOfLength(250)));

        assertInvalidField(withTitle(stringOfLength(251)), "title");
    }

    @Test
    void shouldEnforcePackNotesLengthBoundary() {
        assertValid(withNotes(stringOfLength(1999)));
        assertValid(withNotes(stringOfLength(2000)));

        assertInvalidField(withNotes(stringOfLength(2001)), "notes");
    }

    @Test
    void shouldAcceptNullOptionalPackTitleAndNotes() {
        AppointmentPackGenerationRequest request = new AppointmentPackGenerationRequest(
                UUID.randomUUID(),
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        assertValid(request);
    }

    @ParameterizedTest(name = "{0} should enforce the 100 item selection limit")
    @ValueSource(
            strings = {"medicationIds", "healthcareContactIds", "emergencyContactIds", "medicalHistoryEntryIds", "bloodTestIds"}
    )
    void shouldEnforceSelectionListSizeBoundary(String field) {
        assertValid(withSelection(field, idsOfSize(99)));

        assertValid(withSelection(field, idsOfSize(100)));

        assertInvalidField(withSelection(field, idsOfSize(101)), field);
    }

    @ParameterizedTest(name = "{0} should reject null selection elements")
    @ValueSource(
            strings = {"medicationIds", "healthcareContactIds", "emergencyContactIds", "medicalHistoryEntryIds", "bloodTestIds"}
    )
    void shouldRejectNullSelectionElements(String field) {
        AppointmentPackGenerationRequest request = withSelection(field, Collections.singletonList(null));

        assertInvalid(request);
    }

    @Test
    void shouldAcceptEmptySelectionLists() {
        AppointmentPackGenerationRequest request = new AppointmentPackGenerationRequest(
                UUID.randomUUID(),
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        assertValid(request);
    }

    @Test
    void shouldNormaliseNullSelectionListsToEmptyImmutableLists() {
        AppointmentPackGenerationRequest request = new AppointmentPackGenerationRequest(
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertValid(request);

        assertTrue(request.medicationIds().isEmpty());
        assertTrue(request.healthcareContactIds().isEmpty());
        assertTrue(request.emergencyContactIds().isEmpty());
        assertTrue(request.medicalHistoryEntryIds().isEmpty());
        assertTrue(request.bloodTestIds().isEmpty());

        assertThrows(UnsupportedOperationException.class, () -> request.medicationIds().add(UUID.randomUUID()));
    }

    /**
     * Returns a test request with the title value replaced by the supplied value.
     */
    private static AppointmentPackGenerationRequest withTitle(
            String title) {
        return new AppointmentPackGenerationRequest(
                UUID.randomUUID(),
                title,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    /**
     * Returns a test request with the notes value replaced by the supplied value.
     */
    private static AppointmentPackGenerationRequest withNotes(
            String notes) {
        return new AppointmentPackGenerationRequest(
                UUID.randomUUID(),
                null,
                notes,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    /**
     * Returns the request variants with the selected selection value replaced for the validation test.
     */
    private static AppointmentPackGenerationRequest withSelection(String field, List<UUID> values) {
        return switch (field) {
            case "medicationIds" -> new AppointmentPackGenerationRequest(
                    UUID.randomUUID(),
                    null,
                    null,
                    values,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of());

            case "healthcareContactIds" -> new AppointmentPackGenerationRequest(
                    UUID.randomUUID(),
                    null,
                    null,
                    List.of(),
                    values,
                    List.of(),
                    List.of(),
                    List.of());

            case "emergencyContactIds" -> new AppointmentPackGenerationRequest(
                    UUID.randomUUID(),
                    null,
                    null,
                    List.of(),
                    List.of(),
                    values,
                    List.of(),
                    List.of());

            case "medicalHistoryEntryIds" -> new AppointmentPackGenerationRequest(
                    UUID.randomUUID(),
                    null,
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    values,
                    List.of());

            case "bloodTestIds" -> new AppointmentPackGenerationRequest(
                    UUID.randomUUID(),
                    null,
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    values);

            default -> throw new IllegalArgumentException("Unsupported appointment pack selection field: " + field);
        };
    }

    /**
     * Creates a list of IDs with the requested size for selection limit tests.
     */
    private static List<UUID> idsOfSize(int size) {
        return IntStream.range(0, size).mapToObj(index -> UUID.randomUUID()).toList();
    }
}