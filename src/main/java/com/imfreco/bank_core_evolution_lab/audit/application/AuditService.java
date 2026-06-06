package com.imfreco.bank_core_evolution_lab.audit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imfreco.bank_core_evolution_lab.audit.domain.AuditLog;
import com.imfreco.bank_core_evolution_lab.audit.infrastructure.AuditLogRepository;
import com.imfreco.bank_core_evolution_lab.audit.web.AuditLogResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void record(String operationType, String entityType, String entityId, String actor,
                       String channel, String correlationId, Object details) {
        repository.save(new AuditLog(
                operationType,
                entityType,
                entityId,
                actor,
                channel,
                correlationId,
                toJson(details)
        ));
    }

    public List<AuditLogResponse> find(String entityType, String entityId) {
        List<AuditLog> logs;
        if (entityType != null && !entityType.isBlank() && entityId != null && !entityId.isBlank()) {
            logs = repository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
        } else if (entityType != null && !entityType.isBlank()) {
            logs = repository.findByEntityTypeOrderByCreatedAtDesc(entityType);
        } else {
            logs = repository.findAll();
        }
        return logs.stream().map(this::toResponse).toList();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getOperationType(),
                log.getEntityType(),
                log.getEntityId(),
                log.getActor(),
                log.getChannel(),
                log.getCorrelationId(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }

    private String toJson(Object details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException exception) {
            return "{\"serializationError\":true}";
        }
    }
}
