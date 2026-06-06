package com.imfreco.bank_core_evolution_lab.account.web;

import com.imfreco.bank_core_evolution_lab.account.domain.Account;

public final class AccountMapper {

    private AccountMapper() {
    }

    public static AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCustomerId(),
                account.getType(),
                account.getStatus(),
                account.getCurrency(),
                account.getAccountingBalance(),
                account.getAvailableBalance(),
                account.getVersion(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    public static BalanceResponse toBalanceResponse(Account account) {
        return new BalanceResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCurrency(),
                account.getAccountingBalance(),
                account.getAvailableBalance()
        );
    }
}
