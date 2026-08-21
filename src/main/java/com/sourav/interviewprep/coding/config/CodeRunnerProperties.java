package com.sourav.interviewprep.coding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.coding.runner")
public record CodeRunnerProperties(
        URI baseUrl,
        String apiKey,
        Duration connectTimeout,
        Duration requestTimeout
) {
    public CodeRunnerProperties {
        apiKey = apiKey == null ? "" : apiKey.trim();
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(10) : requestTimeout;
    }
}
