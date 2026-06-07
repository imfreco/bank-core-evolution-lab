package com.imfreco.bank_core_evolution_lab.common.exception;

public class DuplicateUsernameException extends DomainException {

    public DuplicateUsernameException(String username) {
        super("DUPLICATE_USERNAME", "Username is already in use: " + username);
    }
}
