package com.imfreco.bank_core_evolution_lab.common.exception;

public class CurrencyMismatchException extends DomainException {

    public CurrencyMismatchException() {
        super(
                "CURRENCY_MISMATCH",
                "Source account, target account and transfer must use the same currency");
    }
}
