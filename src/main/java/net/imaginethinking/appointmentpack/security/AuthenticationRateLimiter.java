package net.imaginethinking.appointmentpack.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks authentication requests in memory and decides when a client must wait before trying again.
 */
@Component
public class AuthenticationRateLimiter {

    private static final long CLEANUP_INTERVAL = 500;

    private final Map<RateLimitKey, Window> windows = new ConcurrentHashMap<>();

    private final AtomicLong checks = new AtomicLong();

    /**
     * Uses the client and operation as a key, counts requests within the current window and returns a retry delay
     * when the limit is reached.
     */
    public RateLimitDecision check(String clientKey, String operation, int maximumRequests, Duration windowDuration) {
        if (maximumRequests <= 0) {
            throw new IllegalArgumentException("Maximum requests must be positive");
        }

        if (windowDuration == null || windowDuration.isZero() || windowDuration.isNegative()) {
            throw new IllegalArgumentException("Rate-limit window must be positive");
        }

        Instant now = Instant.now();

        RateLimitKey key = new RateLimitKey(clientKey, operation);

        Window window = windows.compute(
                key, (ignored, existing) -> {
                    if (existing == null || !existing.expiresAt().isAfter(now)) {
                        return new Window(1, now.plus(windowDuration));
                    }

                    return new Window(existing.count() + 1, existing.expiresAt());
                });

        // Clear expired windows occasionally instead of scanning the map on every authentication request.
        if (checks.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            removeExpiredWindows(now);
        }

        if (window.count() <= maximumRequests) {
            return RateLimitDecision.permit();
        }

        long retryAfterSeconds = Math.max(1, Duration.between(now, window.expiresAt()).toSeconds());

        return RateLimitDecision.rejected(retryAfterSeconds);
    }

    /**
     * Removes rate limit windows that have already expired.
     */
    private void removeExpiredWindows(Instant now) {
        windows.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    /**
     * Identifies a rate limit window by client and authentication operation.
     */
    private record RateLimitKey(String clientKey, String operation) {
    }

    /**
     * Stores the request count and expiry time for one rate limit window.
     */
    private record Window(int count, Instant expiresAt) {
    }

    /**
     * Reports whether a request is allowed and how long a rejected caller should wait.
     */
    public record RateLimitDecision(boolean allowed, long retryAfterSeconds) {

        /**
         * Creates an allowed rate limit decision with no retry delay.
         */
        public static RateLimitDecision permit() {
            return new RateLimitDecision(true, 0);
        }

        /**
         * Creates a rejected rate limit decision with the retry delay for the caller.
         */
        public static RateLimitDecision rejected(
                long retryAfterSeconds) {
            return new RateLimitDecision(false, retryAfterSeconds);
        }
    }
}