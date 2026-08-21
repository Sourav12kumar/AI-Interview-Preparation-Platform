package com.sourav.interviewprep.common.dto;

import java.time.Instant;

public record ApiHealthResponse(
        String status,
        String service,
        String version,
        Instant timestamp
) {
}
