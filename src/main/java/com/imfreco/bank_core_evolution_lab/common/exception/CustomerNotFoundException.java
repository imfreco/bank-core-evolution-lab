package com.imfreco.bank_core_evolution_lab.common.exception;

import java.util.UUID;

public class CustomerNotFoundException extends DomainException {

    public CustomerNotFoundException(UUID customerId) {
        super("CUSTOMER_NOT_FOUND", "Customer not found: " + customerId);
    }
}
