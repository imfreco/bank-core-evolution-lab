package com.imfreco.bank_core_evolution_lab.transfer.application.port.in;

import com.imfreco.bank_core_evolution_lab.account.domain.Currency;
import com.imfreco.bank_core_evolution_lab.transfer.domain.TransferStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResult(
        String transferReference,
        UUID sourceAccountId,
        String sourceAccountNumber,
        UUID targetAccountId,
        String targetAccountNumber,
        BigDecimal amount,
        Currency currency,
        TransferStatus status,
        Instant createdAt,
        Instant completedAt) {}
