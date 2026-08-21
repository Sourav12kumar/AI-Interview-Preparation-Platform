package com.sourav.interviewprep.common.controller;

import com.sourav.interviewprep.common.dto.ApiHealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final String applicationName;
    private final String applicationVersion;

    public HealthController(
            @Value("${spring.application.name}") String applicationName,
            @Value("${info.app.version:unknown}") String applicationVersion) {
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
    }

    @GetMapping
    public ResponseEntity<ApiHealthResponse> health() {
        return ResponseEntity.ok(new ApiHealthResponse(
                "UP",
                applicationName,
                applicationVersion,
                Instant.now()
        ));
    }
}
