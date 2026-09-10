package net.imaginethinking.appointmentpack.patientcareraccess;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static net.imaginethinking.appointmentpack.testsupport.ValidationTestSupport.*;

/**
 * Checks the validation rules used for patient carer access request.
 */
class PatientCarerAccessRequestValidationTest {

    @Test
    void shouldAcceptRepresentativeCarerInvitationRequest() {
        CreateCarerInvitationRequest request = new CreateCarerInvitationRequest(
                "carer@example.com",
                Set.of("patient-record:view"));

        assertValid(request);
    }

    @Test
    void shouldAcceptCarerEmailAtMaximumLength() {
        assertValid(new CreateCarerInvitationRequest(emailOfLength(253), Set.of("patient-record:view")));

        assertValid(new CreateCarerInvitationRequest(emailOfLength(254), Set.of("patient-record:view")));
    }

    @Test
    void shouldRejectCarerEmailAboveMaximumLength() {
        CreateCarerInvitationRequest request = new CreateCarerInvitationRequest(
                emailOfLength(255),
                Set.of("patient-record:view"));

        assertInvalidField(request, "carerEmail");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "not-an-email"})
    void shouldRejectInvalidCarerEmail(String email) {
        CreateCarerInvitationRequest request = new CreateCarerInvitationRequest(email, Set.of("patient-record:view"));

        assertInvalidField(request, "carerEmail");
    }

    @Test
    void shouldRejectNullInvitationPermissions() {
        CreateCarerInvitationRequest request = new CreateCarerInvitationRequest("carer@example.com", null);

        assertInvalidField(request, "permissions");
    }

    @Test
    void shouldAcceptEmptyInvitationPermissionSetAtValidationLayer() {
        CreateCarerInvitationRequest request = new CreateCarerInvitationRequest("carer@example.com", Set.of());

        assertValid(request);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void shouldRejectBlankInvitationPermissionValues(String permission) {
        CreateCarerInvitationRequest request = new CreateCarerInvitationRequest(
                "carer@example.com",
                Set.of(permission));

        assertInvalid(request);
    }

    @Test
    void shouldLeaveUnsupportedInvitationPermissionsToPermissionValidator() {
        CreateCarerInvitationRequest request = new CreateCarerInvitationRequest(
                "carer@example.com",
                Set.of("unsupported:permission"));

        assertValid(request);
    }

    @Test
    void shouldAcceptRepresentativePermissionUpdateRequest() {
        UpdatePatientCarerPermissionsRequest request = new UpdatePatientCarerPermissionsRequest(Set.of(
                "patient-record:view"));

        assertValid(request);
    }

    @Test
    void shouldRejectNullPermissionUpdateSet() {
        UpdatePatientCarerPermissionsRequest request = new UpdatePatientCarerPermissionsRequest(null);

        assertInvalidField(request, "permissions");
    }

    @Test
    void shouldAcceptEmptyPermissionUpdateSetAtValidationLayer() {
        UpdatePatientCarerPermissionsRequest request = new UpdatePatientCarerPermissionsRequest(Set.of());

        assertValid(request);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void shouldRejectBlankPermissionUpdateValues(String permission) {
        UpdatePatientCarerPermissionsRequest request = new UpdatePatientCarerPermissionsRequest(Set.of(permission));

        assertInvalid(request);
    }

    @Test
    void shouldLeaveUnsupportedUpdatedPermissionsToPermissionValidator() {
        UpdatePatientCarerPermissionsRequest request = new UpdatePatientCarerPermissionsRequest(Set.of(
                "unsupported:permission"));

        assertValid(request);
    }

    /**
     * Creates an email address with the requested length for boundary validation tests.
     */
    private static String emailOfLength(int length) {
        String prefix = "a@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(63) + ".";

        int remainingCharacters = length - prefix.length();

        if (remainingCharacters < 1 || remainingCharacters > 63) {
            throw new IllegalArgumentException("Unsupported synthetic email length: " + length);
        }

        return prefix + "e".repeat(remainingCharacters);
    }
}