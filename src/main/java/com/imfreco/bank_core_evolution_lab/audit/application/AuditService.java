package com.imfreco.bank_core_evolution_lab.audit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imfreco.bank_core_evolution_lab.audit.application.port.in.AuditLogResult;
import com.imfreco.bank_core_evolution_lab.audit.application.port.in.AuditLogUseCase;
import com.imfreco.bank_core_evolution_lab.audit.application.port.out.AuditLogRepositoryPort;
import com.imfreco.bank_core_evolution_lab.audit.application.port.out.AuditRecorderPort;
import com.imfreco.bank_core_evolution_lab.audit.domain.AuditLog;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditService implements AuditLogUseCase, AuditRecorderPort {

    private final AuditLogRepositoryPort repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepositoryPort repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(
            String operationType,
            String entityType,
            String entityId,
            String actor,
            String channel,
            String correlationId,
            Object details) {
        repository.save(
                new AuditLog(
                        operationType,
                        entityType,
                        entityId,
                        actor,
                        channel,
                        correlationId,
                        toJson(details)));
    }

    @Override
    public List<AuditLogResult> find(String entityType, String entityId) {
        List<AuditLog> logs;
        if (entityType != null
                && !entityType.isBlank()
                && entityId != null
                && !entityId.isBlank()) {
            logs = repository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
        } else if (entityType != null && !entityType.isBlank()) {
            logs = repository.findByEntityTypeOrderByCreatedAtDesc(entityType);
        } else {
            logs = repository.findAll();
        }
        return logs.stream().map(this::toResponse).toList();
    }

    private AuditLogResult toResponse(AuditLog log) {
        return new AuditLogResult(
                log.getId(),
                log.getOperationType(),
                log.getEntityType(),
                log.getEntityId(),
                log.getActor(),
                log.getChannel(),
                log.getCorrelationId(),
                log.getDetails(),
                log.getCreatedAt());
    }

    private String toJson(Object details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException exception) {
            return "{\"serializationError\":true}";
        }
    }
}
