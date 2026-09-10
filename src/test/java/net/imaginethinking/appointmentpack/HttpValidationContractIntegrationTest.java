package net.imaginethinking.appointmentpack;

import net.imaginethinking.appointmentpack.event.analytics.ApplicationPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Checks the validation rules used for HTTP contract integration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HttpValidationContractIntegrationTest {

    private static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnValidationProblemForOverlengthRegistrationName() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "email",
                        "patient@example.com",
                        "password",
                        "Appointment1!",
                        "confirmPassword",
                        "Appointment1!",
                        "firstName",
                        "a".repeat(101),
                        "lastName",
                        "User",
                        "dateOfBirth",
                        "1990-01-01"))));

        assertValidationProblem(result, "firstName");
    }

    @Test
    void shouldReturnValidationProblemForWeakRegistrationPassword() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "email",
                        "patient@example.com",
                        "password",
                        "password",
                        "confirmPassword",
                        "password",
                        "firstName",
                        "Patient",
                        "lastName",
                        "User",
                        "dateOfBirth",
                        "1990-01-01"))));

        assertValidationProblem(result, "password");
    }

    @Test
    void shouldReturnValidationProblemForAppointmentWithoutDate() throws Exception {
        ResultActions result = mockMvc.perform(post(
                "/api/v1/patient-records/{patientRecordId}/appointments",
                UUID.randomUUID()).with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("startTime", "10:00"))));

        assertValidationProblem(result, "date");
    }

    @Test
    void shouldReturnValidationProblemForBloodTestAboveResultLimit() throws Exception {
        List<Map<String, Object>> results = IntStream.range(0, 101)
                .mapToObj(index -> Map.<String, Object>of("analyteName", "Analyte " + index, "resultValue", "1"))
                .toList();

        ResultActions result = mockMvc.perform(post(
                "/api/v1/patient-records/{patientRecordId}/blood-tests",
                UUID.randomUUID()).with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("testDate", LocalDate.now().toString(), "results", results))));

        assertValidationProblem(result, "results");
    }

    @Test
    void shouldReturnValidationProblemForFutureMedicalHistoryDate() throws Exception {
        ResultActions result = mockMvc.perform(post(
                "/api/v1/patient-records/{patientRecordId}/medical-history",
                UUID.randomUUID()).with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "title",
                        "Example history entry",
                        "summary",
                        "Synthetic test summary",
                        "entryDate",
                        LocalDate.now().plusDays(1).toString()))));

        assertValidationProblem(result, "entryDate");
    }

    @Test
    void shouldReturnValidationProblemForAppointmentPackAboveSelectionLimit() throws Exception {
        List<UUID> medicationIds = IntStream.range(0, 101).mapToObj(index -> UUID.randomUUID()).toList();

        ResultActions result = mockMvc.perform(post(
                "/api/v1/patient-records/{patientRecordId}/appointment-packs",
                UUID.randomUUID()).with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("appointmentId", UUID.randomUUID(), "medicationIds", medicationIds))));

        assertValidationProblem(result, "medicationIds");
    }

    @Test
    void shouldReturnValidationProblemForInvalidNestedAddress() throws Exception {
        ResultActions result = mockMvc.perform(put("/api/v1/profiles/me").with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "firstName", "Patient", "lastName", "User", "dateOfBirth", "1990-01-01", "address", Map.of(
                                "addressLine1",
                                "",
                                "townCity",
                                "Exampletown",
                                "postcode",
                                "AB1 2CD",
                                "country",
                                "United Kingdom")))));

        assertValidationProblem(result, "address.addressLine1");
    }

    @Test
    void shouldReturnValidationProblemForNegativeAuditPage() throws Exception {
        ResultActions result = mockMvc.perform(get(
                "/api/v1/patient-records/{patientRecordId}/audit-events",
                UUID.randomUUID()).with(userJwt())
                .param("page", "-1")
                .param("size", "50"));

        assertParameterValidationProblem(result, "Page must not be negative");
    }

    @Test
    void shouldReturnValidationProblemForAdminPageSizeAboveMaximum() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/admin/analytics/events").with(adminJwt())
                .param("page", "0")
                .param("size", "101"));

        assertParameterValidationProblem(result, "Page size must not exceed 100");
    }

    @Test
    void shouldRejectUnknownPageViewEnumValue() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/page-views").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "page": "UNKNOWN_PAGE"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldAllowAnonymousAccessToPublicPageViewEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/page-views").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("page", ApplicationPage.LANDING)))).andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectUnauthenticatedAccessToProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectMalformedBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/me").header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectNormalUserFromAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/events").with(userJwt())).andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminThroughSecurityBeforeRequestValidation() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/events").with(adminJwt()).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    /**
     * Checks that the response contains a validation problem for the expected field.
     */
    private void assertValidationProblem(ResultActions result, String field) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors['" + field + "']").exists());
    }

    /**
     * Checks that the response contains the expected parameter validation problem.
     */
    private void assertParameterValidationProblem(ResultActions result, String expectedMessage) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors").isMap())
                .andExpect(content().string(containsString(expectedMessage)));
    }

    /**
     * Returns the user JWT used by the surrounding tests.
     */
    private RequestPostProcessor userJwt() {
        return jwt().jwt(builder -> builder.subject(TEST_USER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * Returns the admin JWT used by the surrounding tests.
     */
    private RequestPostProcessor adminJwt() {
        return jwt().jwt(builder -> builder.subject(TEST_USER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    /**
     * Serialises a test value into JSON for an HTTP request.
     */
    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}