package com.imfreco.bank_core_evolution_lab.audit.application.port.in;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResult(
        UUID id,
        String operationType,
        String entityType,
        String entityId,
        String actor,
        String channel,
        String correlationId,
        String details,
        Instant createdAt) {}
