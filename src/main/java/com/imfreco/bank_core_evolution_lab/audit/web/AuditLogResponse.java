package com.imfreco.bank_core_evolution_lab.audit.web;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String operationType,
        String entityType,
        String entityId,
        String actor,
        String channel,
        String correlationId,
        String details,
        Instant createdAt) {}
