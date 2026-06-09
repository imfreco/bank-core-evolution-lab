package com.imfreco.bank_core_evolution_lab.customer.infrastructure.adapter.in.web;

import com.imfreco.bank_core_evolution_lab.customer.domain.CustomerStatus;
import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String documentType,
        String documentNumber,
        String fullName,
        String email,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt) {}
