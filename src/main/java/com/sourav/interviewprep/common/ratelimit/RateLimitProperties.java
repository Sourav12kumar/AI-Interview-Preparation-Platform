package com.sourav.interviewprep.common.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        Duration window,
        int generalRequests,
        int authRequests,
        int aiRequests,
        int maxTrackedKeys
) {
    public RateLimitProperties {
        window = window == null ? Duration.ofMinutes(1) : window;
        generalRequests = generalRequests < 1 ? 300 : generalRequests;
        authRequests = authRequests < 1 ? 60 : authRequests;
        aiRequests = aiRequests < 1 ? 60 : aiRequests;
        maxTrackedKeys = maxTrackedKeys < 100 ? 10000 : maxTrackedKeys;
    }
}
