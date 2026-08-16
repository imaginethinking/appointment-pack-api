package net.imaginethinking.appointmentpack.document.processing;

import net.imaginethinking.appointmentpack.document.DocumentRepository;
import net.imaginethinking.appointmentpack.document.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingRecoveryServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentProcessingLifecyclePolicy lifecyclePolicy;

    private DocumentProcessingRecoveryService service;

    @BeforeEach
    void setUp() {
        service = new DocumentProcessingRecoveryService(documentRepository, lifecyclePolicy);
    }

    @Test
    void shouldRecoverStaleExtractionAndSummarisationStates() {
        Instant cutoff = Instant.parse("2026-08-14T03:00:00Z");

        when(lifecyclePolicy.staleCutoff(any(Instant.class))).thenReturn(cutoff);

        service.recoverStaleProcessingStates();

        verify(documentRepository).recoverStaleProcessingState(
                eq(DocumentStatus.EXTRACTING),
                eq(DocumentStatus.EXTRACTION_FAILED),
                eq(cutoff),
                any(Instant.class),
                eq(DocumentProcessingLifecyclePolicy.STALE_EXTRACTION_FAILURE_REASON));

        verify(documentRepository).recoverStaleProcessingState(
                eq(DocumentStatus.SUMMARISING),
                eq(DocumentStatus.SUMMARISATION_FAILED),
                eq(cutoff),
                any(Instant.class),
                eq(DocumentProcessingLifecyclePolicy.STALE_SUMMARISATION_FAILURE_REASON));
    }
}