package com.imfreco.bank_core_evolution_lab.customer.web;

import com.imfreco.bank_core_evolution_lab.customer.domain.Customer;

public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getDocumentType(),
                customer.getDocumentNumber(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
