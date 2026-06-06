package com.imfreco.bank_core_evolution_lab.transfer.web;

import com.imfreco.bank_core_evolution_lab.account.domain.Currency;
import com.imfreco.bank_core_evolution_lab.transfer.domain.TransferStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
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
