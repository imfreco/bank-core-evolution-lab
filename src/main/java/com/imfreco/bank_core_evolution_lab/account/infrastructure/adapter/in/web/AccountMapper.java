package com.imfreco.bank_core_evolution_lab.account.infrastructure.adapter.in.web;

import com.imfreco.bank_core_evolution_lab.account.application.port.in.AccountResult;
import com.imfreco.bank_core_evolution_lab.account.application.port.in.BalanceResult;
import com.imfreco.bank_core_evolution_lab.account.application.port.in.CreateAccountCommand;

public final class AccountMapper {

    private AccountMapper() {}

    public static CreateAccountCommand toCommand(AccountRequest request) {
        return new CreateAccountCommand(
                request.customerId(), request.type(), request.currency(), request.initialBalance());
    }

    public static AccountResponse toResponse(AccountResult account) {
        return new AccountResponse(
                account.id(),
                account.accountNumber(),
                account.customerId(),
                account.type(),
                account.status(),
                account.currency(),
                account.accountingBalance(),
                account.availableBalance(),
                account.version(),
                account.createdAt(),
                account.updatedAt());
    }

    public static BalanceResponse toBalanceResponse(BalanceResult balance) {
        return new BalanceResponse(
                balance.accountId(),
                balance.accountNumber(),
                balance.currency(),
                balance.accountingBalance(),
                balance.availableBalance());
    }
}
