package com.imfreco.bank_core_evolution_lab.audit.infrastructure.adapter.in.web;

import com.imfreco.bank_core_evolution_lab.audit.application.port.in.AuditLogResult;
import com.imfreco.bank_core_evolution_lab.audit.application.port.in.AuditLogUseCase;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditController {

    private final AuditLogUseCase auditLogUseCase;

    public AuditController(AuditLogUseCase auditLogUseCase) {
        this.auditLogUseCase = auditLogUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public List<AuditLogResponse> find(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId) {
        return auditLogUseCase.find(entityType, entityId).stream()
                .map(AuditController::toResponse)
                .toList();
    }

    private static AuditLogResponse toResponse(AuditLogResult result) {
        return new AuditLogResponse(
                result.id(),
                result.operationType(),
                result.entityType(),
                result.entityId(),
                result.actor(),
                result.channel(),
                result.correlationId(),
                result.details(),
                result.createdAt());
    }
}
