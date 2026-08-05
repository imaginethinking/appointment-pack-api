package net.imaginethinking.appointmentpack.medicalhistory;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MedicalHistoryController {

    private final MedicalHistoryService medicalHistoryService;

    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @GetMapping("/patient-records/{patientRecordId}/medical-history")
    public ResponseEntity<List<MedicalHistoryEntryResponse>> getMedicalHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientRecordId) {
        UUID authenticatedUserId = authenticatedUserIdResolver.resolve(jwt);

        List<MedicalHistoryEntryResponse> response = medicalHistoryService.getHistory(
                authenticatedUserId,
                patientRecordId);

        return ResponseEntity.ok(response);
    }
}