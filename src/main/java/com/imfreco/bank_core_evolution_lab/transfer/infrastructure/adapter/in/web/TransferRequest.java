package com.imfreco.bank_core_evolution_lab.transfer.infrastructure.adapter.in.web;

import com.imfreco.bank_core_evolution_lab.account.domain.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank String sourceAccountNumber,
        @NotBlank String targetAccountNumber,
        @NotNull @Positive BigDecimal amount,
        @NotNull Currency currency) {}
