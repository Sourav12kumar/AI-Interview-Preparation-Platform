package com.sourav.interviewprep.admin.dto;

import com.sourav.interviewprep.admin.audit.AuditOutcome;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record AuditEventResponse(
        Long id,
        Long actorUserId,
        String action,
        String targetType,
        String targetId,
        AuditOutcome outcome,
        String correlationId,
        JsonNode metadata,
        Instant createdAt
) {
}
