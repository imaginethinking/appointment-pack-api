package net.imaginethinking.appointmentpack.document.processing;

import net.imaginethinking.appointmentpack.document.Document;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks the document processing lifecycle policy behaviour covered by this test class.
 */
class DocumentProcessingLifecyclePolicyTest {

    private static final Duration STALE_AFTER = Duration.ofMinutes(5);

    private final DocumentProcessingLifecyclePolicy policy = new DocumentProcessingLifecyclePolicy(STALE_AFTER);

    @Test
    void shouldCalculateStaleCutoffFromConfiguredDuration() {
        Instant now = Instant.parse("2026-08-24T12:00:00Z");

        assertEquals(now.minus(STALE_AFTER), policy.staleCutoff(now));
    }

    @Test
    void shouldNotTreatDocumentAsStaleImmediatelyBeforeBoundary() {
        Instant now = Instant.parse("2026-08-24T12:00:00Z");

        Document document = documentUpdatedAt(now.minus(STALE_AFTER).plusNanos(1));

        assertFalse(policy.isStale(document, now));
    }

    @Test
    void shouldTreatDocumentAsStaleExactlyAtBoundary() {
        Instant now = Instant.parse("2026-08-24T12:00:00Z");

        Document document = documentUpdatedAt(now.minus(STALE_AFTER));

        assertTrue(policy.isStale(document, now));
    }

    @Test
    void shouldTreatDocumentAsStaleImmediatelyAfterBoundary() {
        Instant now = Instant.parse("2026-08-24T12:00:00Z");

        Document document = documentUpdatedAt(now.minus(STALE_AFTER).minusNanos(1));

        assertTrue(policy.isStale(document, now));
    }

    @Test
    void shouldNotTreatDocumentWithoutUpdateTimestampAsStale() {
        assertFalse(policy.isStale(new Document(), Instant.parse("2026-08-24T12:00:00Z")));
    }

    @Test
    void shouldRejectNullStaleDuration() {
        assertThrows(IllegalArgumentException.class, () -> new DocumentProcessingLifecyclePolicy(null));
    }

    @Test
    void shouldRejectZeroStaleDuration() {
        assertThrows(IllegalArgumentException.class, () -> new DocumentProcessingLifecyclePolicy(Duration.ZERO));
    }

    @Test
    void shouldRejectNegativeStaleDuration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentProcessingLifecyclePolicy(Duration.ofSeconds(-1)));
    }

    /**
     * Creates a document with the supplied update time for lifecycle recovery tests.
     */
    private Document documentUpdatedAt(Instant updatedAt) {
        Document document = new Document();

        ReflectionTestUtils.setField(document, "updatedAt", updatedAt);

        return document;
    }
}