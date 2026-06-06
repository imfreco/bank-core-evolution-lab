package com.imfreco.bank_core_evolution_lab.common.exception;

public class AccountNotFoundException extends DomainException {

    public AccountNotFoundException(String accountIdentifier) {
        super("ACCOUNT_NOT_FOUND", "Account not found: " + accountIdentifier);
    }
}
