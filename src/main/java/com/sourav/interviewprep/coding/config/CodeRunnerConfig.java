package com.sourav.interviewprep.coding.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CodeRunnerProperties.class)
public class CodeRunnerConfig {
}
