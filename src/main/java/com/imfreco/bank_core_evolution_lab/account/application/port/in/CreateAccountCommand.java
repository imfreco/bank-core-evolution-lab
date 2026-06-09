package com.imfreco.bank_core_evolution_lab.account.application.port.in;

import com.imfreco.bank_core_evolution_lab.account.domain.AccountType;
import com.imfreco.bank_core_evolution_lab.account.domain.Currency;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountCommand(
        UUID customerId, AccountType type, Currency currency, BigDecimal initialBalance) {}
