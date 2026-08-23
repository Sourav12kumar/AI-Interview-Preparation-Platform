package com.sourav.interviewprep.interview.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(String apiKey, String model) {
    public GeminiProperties {
        apiKey = apiKey == null ? "" : apiKey.trim();
        model = model == null || model.isBlank() ? "gemini-2.5-flash" : model.trim();
    }
}
