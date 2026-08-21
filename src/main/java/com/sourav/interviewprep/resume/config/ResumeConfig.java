package com.sourav.interviewprep.resume.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ResumeProperties.class)
public class ResumeConfig {
}
