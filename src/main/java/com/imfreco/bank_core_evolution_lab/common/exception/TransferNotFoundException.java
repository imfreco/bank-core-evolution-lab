package com.imfreco.bank_core_evolution_lab.common.exception;

public class TransferNotFoundException extends DomainException {

    public TransferNotFoundException(String transferReference) {
        super("TRANSFER_NOT_FOUND", "Transfer not found: " + transferReference);
    }
}
