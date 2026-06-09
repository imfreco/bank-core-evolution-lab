package com.imfreco.bank_core_evolution_lab.transfer.application.port.in;

import com.imfreco.bank_core_evolution_lab.account.domain.Currency;
import com.imfreco.bank_core_evolution_lab.common.application.security.AuthenticatedActor;
import java.math.BigDecimal;

public record CreateTransferCommand(
        String sourceAccountNumber,
        String targetAccountNumber,
        BigDecimal amount,
        Currency currency,
        String idempotencyKey,
        AuthenticatedActor actor,
        String channel,
        String correlationId) {}
