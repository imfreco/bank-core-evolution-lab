package com.imfreco.bank_core_evolution_lab.audit.infrastructure;

import com.imfreco.bank_core_evolution_lab.audit.domain.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, String entityId);

    List<AuditLog> findByEntityTypeOrderByCreatedAtDesc(String entityType);
}
