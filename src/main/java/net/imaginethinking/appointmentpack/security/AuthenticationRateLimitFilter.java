package net.imaginethinking.appointmentpack.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {

    private static final Map<String, RateLimitPolicy> POLICIES = Map.ofEntries(
            Map.entry("/api/v1/auth/register", new RateLimitPolicy(5, Duration.ofHours(1))),
            Map.entry("/api/v1/auth/login", new RateLimitPolicy(20, Duration.ofMinutes(5))),
            Map.entry("/api/v1/auth/login/mfa", new RateLimitPolicy(20, Duration.ofMinutes(5))),
            Map.entry("/api/v1/auth/email-verification/resend", new RateLimitPolicy(5, Duration.ofMinutes(15))),
            Map.entry("/api/v1/auth/email-verification/confirm", new RateLimitPolicy(10, Duration.ofMinutes(15))),
            Map.entry("/api/v1/auth/password-reset/request", new RateLimitPolicy(5, Duration.ofMinutes(15))),
            Map.entry("/api/v1/auth/password-reset/confirm", new RateLimitPolicy(10, Duration.ofMinutes(15))),
            Map.entry("/api/v1/auth/password/change", new RateLimitPolicy(10, Duration.ofMinutes(5))),
            Map.entry("/api/v1/auth/mfa/setup", new RateLimitPolicy(10, Duration.ofMinutes(5))),
            Map.entry("/api/v1/auth/mfa/confirm", new RateLimitPolicy(10, Duration.ofMinutes(5))),
            Map.entry("/api/v1/auth/mfa/disable", new RateLimitPolicy(10, Duration.ofMinutes(5)))
    );

    private final AuthenticationRateLimiter authenticationRateLimiter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }

        return !POLICIES.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        RateLimitPolicy policy = POLICIES.get(request.getRequestURI());

        AuthenticationRateLimiter.RateLimitDecision decision = authenticationRateLimiter.check(
                request.getRemoteAddr(),
                request.getRequestURI(),
                policy.maximumRequests(),
                policy.windowDuration());

        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write("""
                {"type":"about:blank","title":"Too many requests","status":429,"detail":"Too many authentication requests. Try again later."}
                """);
    }

    private record RateLimitPolicy(int maximumRequests, Duration windowDuration) {
    }
}