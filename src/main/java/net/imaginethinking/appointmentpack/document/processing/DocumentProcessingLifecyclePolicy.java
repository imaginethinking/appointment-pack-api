package net.imaginethinking.appointmentpack.document.processing;

import net.imaginethinking.appointmentpack.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Calculates when a document processing state is old enough to be treated as stale.
 */
@Component
public class DocumentProcessingLifecyclePolicy {

    public static final String STALE_EXTRACTION_FAILURE_REASON = "Previous extraction attempt expired before completion.";
    public static final String STALE_SUMMARISATION_FAILURE_REASON = "Previous summarisation attempt expired before completion.";

    private final Duration staleAfter;

    /**
     * Prevents the utility class from being instantiated.
     */
    public DocumentProcessingLifecyclePolicy(
            @Value("${appointment-pack.document-processing.lifecycle.stale-after:PT5M}")
            Duration staleAfter) {
        if (staleAfter == null || staleAfter.isZero() || staleAfter.isNegative()) {
            throw new IllegalArgumentException("Document processing stale duration must be positive");
        }

        this.staleAfter = staleAfter;
    }

    /**
     * Subtracts the configured stale duration from the supplied time to produce the recovery cutoff.
     */
    public Instant staleCutoff(Instant now) {
        return now.minus(staleAfter);
    }

    /**
     * Checks whether a processing document was last updated before the current stale cutoff.
     */
    public boolean isStale(Document document, Instant now) {
        if (document.getUpdatedAt() == null) {
            return false;
        }

        return !document.getUpdatedAt().plus(staleAfter).isAfter(now);
    }
}