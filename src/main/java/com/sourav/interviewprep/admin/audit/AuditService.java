package com.sourav.interviewprep.admin.audit;

import com.sourav.interviewprep.auth.entity.UserEntity;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
public class AuditService {

    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public AuditService(
            AuditEventRepository repository,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    public void success(
            UserEntity actor,
            String action,
            String targetType,
            String targetId,
            Map<String, ?> metadata) {
        String json;
        try {
            json = objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (Exception exception) {
            throw new IllegalStateException("Audit metadata could not be serialized", exception);
        }
        repository.save(new AuditEventEntity(
                actor, action, targetType, targetId, AuditOutcome.SUCCESS,
                MDC.get("correlationId"), json));
        meterRegistry.counter("admin_audit_events_total", "action", action).increment();
    }
}
