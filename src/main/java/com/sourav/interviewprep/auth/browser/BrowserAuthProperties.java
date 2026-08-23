package com.sourav.interviewprep.auth.browser;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record BrowserAuthProperties(boolean browserCookieSecure) {
}
