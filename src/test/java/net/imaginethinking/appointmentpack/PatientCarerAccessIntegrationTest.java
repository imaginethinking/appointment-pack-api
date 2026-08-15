package net.imaginethinking.appointmentpack;

import net.imaginethinking.appointmentpack.user.User;
import net.imaginethinking.appointmentpack.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PatientCarerAccessIntegrationTest {

    private static final String TEST_PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldEnforceCarerPermissionsForPatientRecord() throws Exception {
        String uniqueId = UUID.randomUUID().toString();

        String patientEmail = "patient-" + uniqueId + "@example.com";
        String carerEmail = "carer-" + uniqueId + "@example.com";

        UUID patientUserId = register(patientEmail, "Patient", "User", "1980-01-01");

        UUID carerUserId = register(carerEmail, "Carer", "User", "1985-01-01");

        verifyEmail(patientUserId);
        verifyEmail(carerUserId);

        String patientToken = login(patientEmail);
        String carerToken = login(carerEmail);

        UUID patientRecordId = createPatientRecord(patientToken);

        mockMvc.perform(get("/api/v1/patient-records/{patientRecordId}", patientRecordId).header(
                "Authorization",
                bearer(patientToken))).andExpect(status().isOk());

        UUID accessId = inviteCarer(patientToken, carerEmail, Set.of("patient-record:view"));

        mockMvc.perform(patch("/api/v1/patient-carer-access/{accessId}/accept", accessId).header(
                        "Authorization",
                        bearer(carerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/patient-records/{patientRecordId}", patientRecordId).header(
                "Authorization",
                bearer(carerToken))).andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/patient-records/{patientRecordId}", patientRecordId).header(
                        "Authorization",
                        bearer(carerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nhsNumber", "9434765919")))).andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/patient-carer-access/{accessId}/permissions", accessId).header(
                                "Authorization",
                                bearer(patientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("permissions", Set.of("patient-record:view", "patient-record:edit")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions").isArray());

        mockMvc.perform(put("/api/v1/patient-records/{patientRecordId}", patientRecordId).header(
                                "Authorization",
                                bearer(carerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nhsNumber", "9434765919"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nhsNumber").value("9434765919"));

        mockMvc.perform(patch("/api/v1/patient-carer-access/{accessId}/revoke", accessId).header(
                        "Authorization",
                        bearer(patientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));

        mockMvc.perform(get("/api/v1/patient-records/{patientRecordId}", patientRecordId).header(
                "Authorization",
                bearer(carerToken))).andExpect(status().isForbidden());
    }

    private UUID register(String email, String firstName, String lastName, String dateOfBirth) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", TEST_PASSWORD,
                                "confirmPassword", TEST_PASSWORD,
                                "firstName", firstName,
                                "lastName", lastName,
                                "dateOfBirth", dateOfBirth))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailVerificationRequired").value(true))
                .andReturn();

        return readUuid(result, "id", "userId");
    }

    private void verifyEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Registered integration-test user was not found"));

        user.setEmailVerifiedAt(Instant.now());
        userRepository.saveAndFlush(user);
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        JsonNode response = readResponse(result);
        JsonNode token = response.get("accessToken");

        if (token == null || token.isNull()) {
            throw new IllegalStateException("Login response did not contain accessToken");
        }

        return token.asText();
    }

    private UUID createPatientRecord(String patientToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/patient-records").header("Authorization", bearer(patientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();

        return readUuid(result, "id", "patientRecordId");
    }

    private UUID inviteCarer(String patientToken, String carerEmail, Set<String> permissions) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/patient-carer-access").header(
                                "Authorization",
                                bearer(patientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("carerEmail", carerEmail, "permissions", permissions))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        return readUuid(result, "id", "accessId");
    }

    private UUID readUuid(MvcResult result, String... possibleFields) throws Exception {
        JsonNode response = readResponse(result);

        for (String field : possibleFields) {
            JsonNode value = response.get(field);

            if (value != null && !value.isNull()) {
                return UUID.fromString(value.asText());
            }
        }

        throw new IllegalStateException("Response did not contain an expected UUID field: " + response);
    }

    private JsonNode readResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}