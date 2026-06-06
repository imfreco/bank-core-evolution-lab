package com.imfreco.bank_core_evolution_lab.common.exception;

public class AccountBlockedException extends DomainException {

    public AccountBlockedException(String accountNumber) {
        super("ACCOUNT_BLOCKED", "Account is not active: " + accountNumber);
    }
}
