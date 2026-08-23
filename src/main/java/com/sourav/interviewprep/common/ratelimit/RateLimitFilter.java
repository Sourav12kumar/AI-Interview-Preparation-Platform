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
