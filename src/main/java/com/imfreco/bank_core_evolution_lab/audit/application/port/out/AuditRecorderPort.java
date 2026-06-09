package com.imfreco.bank_core_evolution_lab.audit.application.port.out;

public interface AuditRecorderPort {

    void record(
            String operationType,
            String entityType,
            String entityId,
            String actor,
            String channel,
            String correlationId,
            Object details);
}
