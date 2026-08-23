package com.sourav.interviewprep.common.ratelimit;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Map<String, WindowEntry> counters = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitFilter(
            RateLimitProperties properties,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper) {
        this(properties, meterRegistry, objectMapper, Clock.systemUTC());
    }

    RateLimitFilter(
            RateLimitProperties properties,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper,
            Clock clock) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !properties.enabled()
                || path.equals("/api/v1/health")
                || path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        RateScope scope = scope(request);
        int limit = limit(scope);
        long now = clock.millis();
        long windowMillis = properties.window().toMillis();
        removeExpired(now, windowMillis);

        String identity = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        String rawKey = scope.name() + ":" + identity;
        String key = counters.size() >= properties.maxTrackedKeys() && !counters.containsKey(rawKey)
                ? scope.name() + ":overflow"
                : rawKey;
        WindowEntry entry = counters.compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAtMillis() >= windowMillis) {
                return new WindowEntry(now, 1);
            }
            return new WindowEntry(current.startedAtMillis(), current.count() + 1);
        });

        int remaining = Math.max(0, limit - entry.count());
        response.setHeader("X-RateLimit-Limit", Integer.toString(limit));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(remaining));
        if (entry.count() <= limit) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryMillis = Math.max(1, windowMillis - (now - entry.startedAtMillis()));
        long retrySeconds = Math.max(1, (retryMillis + 999) / 1000);
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(retrySeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "timestamp", Instant.ofEpochMilli(now).toString(),
                "status", 429,
                "error", "Too Many Requests",
                "message", "Request rate limit exceeded"));
        meterRegistry.counter("rate_limit_rejections_total", "scope", scope.name().toLowerCase())
                .increment();
    }

    private RateScope scope(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/auth/")) return RateScope.AUTH;
        if ("POST".equalsIgnoreCase(request.getMethod()) && (
                path.equals("/api/v1/interviews")
                        || path.contains("/answers")
                        || path.matches("/api/v1/resumes/[^/]+/analysis")
                        || path.equals("/api/v1/analytics/reports")
                        || (path.contains("/coding/problems/") && path.endsWith("/submissions")))) {
            return RateScope.AI;
        }
        return RateScope.GENERAL;
    }

    private int limit(RateScope scope) {
        return switch (scope) {
            case AUTH -> properties.authRequests();
            case AI -> properties.aiRequests();
            case GENERAL -> properties.generalRequests();
        };
    }

    private void removeExpired(long now, long windowMillis) {
        if (counters.size() < properties.maxTrackedKeys() / 2) return;
        counters.entrySet().removeIf(entry -> now - entry.getValue().startedAtMillis() >= windowMillis);
    }

    private enum RateScope {
        GENERAL,
        AUTH,
        AI
    }

    private record WindowEntry(long startedAtMillis, int count) {
    }
}
