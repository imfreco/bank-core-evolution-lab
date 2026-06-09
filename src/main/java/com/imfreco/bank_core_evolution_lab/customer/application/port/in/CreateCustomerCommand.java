package com.imfreco.bank_core_evolution_lab.customer.application.port.in;

public record CreateCustomerCommand(
        String documentType,
        String documentNumber,
        String fullName,
        String email,
        String username,
        String password) {}
