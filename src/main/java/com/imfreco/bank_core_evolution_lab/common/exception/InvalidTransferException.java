package com.imfreco.bank_core_evolution_lab.common.exception;

public class InvalidTransferException extends DomainException {

    public InvalidTransferException(String message) {
        super("INVALID_TRANSFER", message);
    }
}
