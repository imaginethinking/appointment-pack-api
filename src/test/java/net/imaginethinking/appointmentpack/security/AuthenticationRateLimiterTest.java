package net.imaginethinking.appointmentpack.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks the authentication rate limiter behaviour covered by this test class.
 */
class AuthenticationRateLimiterTest {

    private final AuthenticationRateLimiter rateLimiter = new AuthenticationRateLimiter();

    @Test
    void shouldAllowRequestsWithinLimit() {
        AuthenticationRateLimiter.RateLimitDecision first = rateLimiter.check(
                "127.0.0.1",
                "/api/v1/auth/login",
                2,
                Duration.ofMinutes(1));

        AuthenticationRateLimiter.RateLimitDecision second = rateLimiter.check(
                "127.0.0.1",
                "/api/v1/auth/login",
                2,
                Duration.ofMinutes(1));

        assertTrue(first.allowed());
        assertTrue(second.allowed());
    }

    @Test
    void shouldRejectRequestsAboveLimit() {
        rateLimiter.check("127.0.0.1", "/api/v1/auth/login", 1, Duration.ofMinutes(1));

        AuthenticationRateLimiter.RateLimitDecision rejected = rateLimiter.check(
                "127.0.0.1",
                "/api/v1/auth/login",
                1,
                Duration.ofMinutes(1));

        assertFalse(rejected.allowed());
        assertTrue(rejected.retryAfterSeconds() > 0);
    }

    @Test
    void shouldRejectInvalidRateLimitConfiguration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> rateLimiter.check("127.0.0.1", "/api/v1/auth/login", 0, Duration.ofMinutes(1)));
    }
}