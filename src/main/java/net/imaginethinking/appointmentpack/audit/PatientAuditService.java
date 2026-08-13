package net.imaginethinking.appointmentpack.audit;

import lombok.RequiredArgsConstructor;
import net.imaginethinking.appointmentpack.patientrecord.PatientRecordAccessService;
import net.imaginethinking.appointmentpack.user.User;
import net.imaginethinking.appointmentpack.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientAuditService {

    private final PatientAuditEventRepository patientAuditEventRepository;
    private final PatientRecordAccessService patientRecordAccessService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PatientAuditPageResponse getAuditEvents(UUID authenticatedUserId, UUID patientRecordId, int page, int size) {
        patientRecordAccessService.requireAccess(
                authenticatedUserId,
                patientRecordId,
                AuditPermission.VIEW
        );

        Page<PatientAuditEvent> auditPage = patientAuditEventRepository
                .findAllByPatientRecordIdOrderByOccurredAtDesc(
                        patientRecordId,
                        PageRequest.of(page, size)
                );

        Set<UUID> actorUserIds = auditPage.getContent()
                .stream()
                .map(PatientAuditEvent::getActorUserId)
                .collect(Collectors.toSet());

        Map<UUID, User> actorsById = actorUserIds.isEmpty() ? Map.of() : userRepository.findAllByIdIn(actorUserIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return new PatientAuditPageResponse(
                auditPage.getContent()
                        .stream()
                        .map(auditEvent -> PatientAuditResponse.from(
                                auditEvent,
                                actorsById.get(auditEvent.getActorUserId())))
                        .toList(),
                auditPage.getNumber(),
                auditPage.getSize(),
                auditPage.getTotalElements(),
                auditPage.getTotalPages());
    }
}