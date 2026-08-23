package com.sourav.interviewprep.auth.browser;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BrowserAuthProperties.class)
public class BrowserAuthConfig {
}
