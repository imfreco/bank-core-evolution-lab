package com.imfreco.bank_core_evolution_lab.movement.application.port.in;

import com.imfreco.bank_core_evolution_lab.account.domain.Currency;
import com.imfreco.bank_core_evolution_lab.movement.domain.MovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MovementResult(
        UUID id,
        UUID accountId,
        String transferReference,
        MovementType type,
        BigDecimal amount,
        Currency currency,
        BigDecimal balanceAfterMovement,
        String description,
        Instant createdAt) {}
