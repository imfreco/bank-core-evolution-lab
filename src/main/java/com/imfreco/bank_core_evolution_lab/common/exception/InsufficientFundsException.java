package com.imfreco.bank_core_evolution_lab.common.exception;

public class InsufficientFundsException extends DomainException {

    public InsufficientFundsException() {
        super("INSUFFICIENT_FUNDS", "Source account has insufficient available balance");
    }
}
