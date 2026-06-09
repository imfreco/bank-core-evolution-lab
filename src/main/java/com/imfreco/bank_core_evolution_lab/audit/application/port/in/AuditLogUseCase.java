package com.imfreco.bank_core_evolution_lab.audit.application.port.in;

import java.util.List;

public interface AuditLogUseCase {

    List<AuditLogResult> find(String entityType, String entityId);
}
