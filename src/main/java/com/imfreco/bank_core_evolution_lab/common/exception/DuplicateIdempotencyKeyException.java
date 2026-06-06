package com.imfreco.bank_core_evolution_lab.common.exception;

public class DuplicateIdempotencyKeyException extends DomainException {

    public DuplicateIdempotencyKeyException(String message) {
        super("DUPLICATE_IDEMPOTENCY_KEY", message);
    }
}
