package com.imfreco.bank_core_evolution_lab.common.exception;

public class AuthenticationFailedException extends DomainException {

    public AuthenticationFailedException() {
        super("INVALID_CREDENTIALS", "Invalid username or password");
    }
}
