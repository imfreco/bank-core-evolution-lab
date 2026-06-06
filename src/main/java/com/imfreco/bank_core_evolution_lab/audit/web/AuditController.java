package com.imfreco.bank_core_evolution_lab.audit.web;

import com.imfreco.bank_core_evolution_lab.audit.application.AuditService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public List<AuditLogResponse> find(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId) {
        return auditService.find(entityType, entityId);
    }
}
