package com.imfreco.bank_core_evolution_lab.audit.application.port.out;

import com.imfreco.bank_core_evolution_lab.audit.domain.AuditLog;
import java.util.List;

public interface AuditLogRepositoryPort {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, String entityId);

    List<AuditLog> findByEntityTypeOrderByCreatedAtDesc(String entityType);

    List<AuditLog> findAll();
}
