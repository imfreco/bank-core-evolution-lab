package com.imfreco.bank_core_evolution_lab.account.infrastructure.adapter.in.web;

import com.imfreco.bank_core_evolution_lab.account.domain.AccountType;
import com.imfreco.bank_core_evolution_lab.account.domain.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

public record AccountRequest(
        @NotNull UUID customerId,
        @NotNull AccountType type,
        @NotNull Currency currency,
        @NotNull @PositiveOrZero BigDecimal initialBalance) {}
