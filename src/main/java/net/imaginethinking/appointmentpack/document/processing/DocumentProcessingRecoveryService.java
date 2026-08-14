package net.imaginethinking.appointmentpack.document.processing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.imaginethinking.appointmentpack.document.DocumentRepository;
import net.imaginethinking.appointmentpack.document.DocumentStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingRecoveryService {

    private final DocumentRepository documentRepository;
    private final DocumentProcessingLifecyclePolicy lifecyclePolicy;

    @Scheduled(fixedDelayString = "${appointment-pack.document-processing.lifecycle.recovery-interval-ms:60000}")
    @Transactional
    public void recoverStaleProcessingStates() {
        Instant now = Instant.now();
        Instant cutoff = lifecyclePolicy.staleCutoff(now);

        int recoveredExtractions = documentRepository.recoverStaleProcessingState(
                DocumentStatus.EXTRACTING,
                DocumentStatus.EXTRACTION_FAILED,
                cutoff,
                now,
                DocumentProcessingLifecyclePolicy.STALE_EXTRACTION_FAILURE_REASON);

        int recoveredSummarisations = documentRepository.recoverStaleProcessingState(
                DocumentStatus.SUMMARISING,
                DocumentStatus.SUMMARISATION_FAILED,
                cutoff,
                now,
                DocumentProcessingLifecyclePolicy.STALE_SUMMARISATION_FAILURE_REASON);

        int totalRecovered = recoveredExtractions + recoveredSummarisations;

        if (totalRecovered > 0) {
            log.info("Recovered {} stale document processing operation(s)", totalRecovered);
        }
    }
}