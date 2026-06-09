package com.imfreco.bank_core_evolution_lab.customer.infrastructure.adapter.in.web;

import com.imfreco.bank_core_evolution_lab.account.infrastructure.adapter.in.web.AccountMapper;
import com.imfreco.bank_core_evolution_lab.customer.application.port.in.CreateCustomerCommand;
import com.imfreco.bank_core_evolution_lab.customer.application.port.in.CustomerProductsResult;
import com.imfreco.bank_core_evolution_lab.customer.application.port.in.CustomerResult;

public final class CustomerMapper {

    private CustomerMapper() {}

    public static CreateCustomerCommand toCommand(CustomerRequest request) {
        return new CreateCustomerCommand(
                request.documentType(),
                request.documentNumber(),
                request.fullName(),
                request.email(),
                request.username(),
                request.password());
    }

    public static CustomerResponse toResponse(CustomerResult customer) {
        return new CustomerResponse(
                customer.id(),
                customer.documentType(),
                customer.documentNumber(),
                customer.fullName(),
                customer.email(),
                customer.status(),
                customer.createdAt(),
                customer.updatedAt());
    }

    public static CustomerProductsResponse toProductsResponse(CustomerProductsResult result) {
        return new CustomerProductsResponse(
                result.customerId(),
                result.accounts().stream().map(AccountMapper::toResponse).toList());
    }
}
