package com.imfreco.bank_core_evolution_lab.customer.application.port.in;

import com.imfreco.bank_core_evolution_lab.customer.domain.CustomerStatus;
import java.time.Instant;
import java.util.UUID;

public record CustomerResult(
        UUID id,
        String documentType,
        String documentNumber,
        String fullName,
        String email,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt) {}
