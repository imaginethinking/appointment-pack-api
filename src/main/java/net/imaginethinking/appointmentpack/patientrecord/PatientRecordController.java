package net.imaginethinking.appointmentpack.patientrecord;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.security.AuthenticatedUserIdResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patient-records")
@RequiredArgsConstructor
public class PatientRecordController {

    private final PatientRecordService patientRecordService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @PostMapping
    public ResponseEntity<PatientRecordResponse> createCurrentPatientRecord(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid CreatePatientRecordRequest request) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        PatientRecordResponse response = patientRecordService.createCurrentPatientRecord(userId, request);

        return ResponseEntity
                .created(URI.create("/api/v1/patient-records/me"))
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<PatientRecordResponse> getCurrentPatientRecord(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = authenticatedUserIdResolver.resolve(jwt);

        PatientRecordResponse response = patientRecordService.getCurrentPatientRecord(userId);

        return ResponseEntity.ok(response);
    }


}
