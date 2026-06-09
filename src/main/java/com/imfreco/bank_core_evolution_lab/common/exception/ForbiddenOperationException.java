package com.imfreco.bank_core_evolution_lab.common.exception;

public class ForbiddenOperationException extends DomainException {

    public ForbiddenOperationException(String message) {
        super("FORBIDDEN", message);
    }
}
