package com.imfreco.bank_core_evolution_lab.account.web;

import com.imfreco.bank_core_evolution_lab.account.domain.AccountStatus;
import com.imfreco.bank_core_evolution_lab.account.domain.AccountType;
import com.imfreco.bank_core_evolution_lab.account.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        UUID customerId,
        AccountType type,
        AccountStatus status,
        Currency currency,
        BigDecimal accountingBalance,
        BigDecimal availableBalance,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
